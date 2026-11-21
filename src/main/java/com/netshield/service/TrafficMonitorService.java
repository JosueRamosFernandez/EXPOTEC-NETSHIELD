package com.netshield.service;

import com.netshield.model.AlertLevel;
import com.netshield.model.PortTrafficSummary;
import com.netshield.model.TrafficConnection;
import com.netshield.model.TrafficSnapshot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * <h2>Motor de Monitoreo de Conexiones y Tráfico en Tiempo Real</h2>
 * <p>
 * Diseñado bajo especificación Java 17 pura sin requerir dependencias de controladores
 * nativos externos (como WinPcap o Npcap). Utiliza telemetría de sockets del sistema
 * operativo y el subsistema de red para auditar conexiones activas, estados TCP,
 * tasas de transferencia (KB/s) y distribución de tráfico por puertos.
 * </p>
 */
public class TrafficMonitorService {

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<TrafficSnapshot> onTrafficUpdate;
    private Consumer<Exception> onError;

    // Métricas previas para calcular derivadas de velocidad (Bytes/s)
    private long lastTimestamp = 0;
    private long lastBytesIn = 0;
    private long lastBytesOut = 0;

    // Mapa acumulativo de conexiones para persistir estadísticas de paquetes
    private final Map<String, TrafficConnection> connectionCache = new ConcurrentHashMap<>();

    // Umbrales configurables de detección de saturación por puerto
    private volatile int warningPortThreshold = 10;
    private volatile int criticalPortThreshold = 25;
    private final Map<Integer, Long> portAlertCooldown = new ConcurrentHashMap<>();
    private long lastBurstAlertTime = 0;

    public void setSaturationThresholds(int warningConns, int criticalConns) {
        this.warningPortThreshold = Math.max(2, warningConns);
        this.criticalPortThreshold = Math.max(this.warningPortThreshold + 2, criticalConns);
    }

    public int getWarningPortThreshold() {
        return warningPortThreshold;
    }

    public int getCriticalPortThreshold() {
        return criticalPortThreshold;
    }

    public void setOnTrafficUpdate(Consumer<TrafficSnapshot> cb) {
        this.onTrafficUpdate = cb;
    }

    public void setOnError(Consumer<Exception> cb) {
        this.onError = cb;
    }

    /**
     * Inicia el ciclo periódico de muestreo de tráfico en segundo plano.
     *
     * @param intervalSeconds Intervalo de sondeo en segundos (típicamente 1 a 3 segundos).
     */
    public synchronized void startMonitoring(int intervalSeconds) {
        if (running.get()) return;

        running.set(true);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NetShield-TrafficMonitor");
            t.setDaemon(true);
            return t;
        });

        // Inicializar lecturas base de bytes de interfaz
        long[] initialStats = readInterfaceBytes();
        lastBytesIn = initialStats[0];
        lastBytesOut = initialStats[1];
        lastTimestamp = System.currentTimeMillis();

        scheduler.scheduleAtFixedRate(this::pollNetworkTraffic, 0, Math.max(1, intervalSeconds), TimeUnit.SECONDS);
    }

    public synchronized void stopMonitoring() {
        running.set(false);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        connectionCache.clear();
    }

    public boolean isMonitoring() {
        return running.get();
    }

    /**
     * Ciclo de adquisición de telemetría de red.
     */
    private void pollNetworkTraffic() {
        if (!running.get()) return;

        try {
            long now = System.currentTimeMillis();
            double deltaSec = (lastTimestamp > 0) ? (now - lastTimestamp) / 1000.0 : 1.0;
            if (deltaSec <= 0) deltaSec = 1.0;

            // 1. Leer estadísticas de bytes del adaptador
            long[] currentBytes = readInterfaceBytes();
            double downKbps = 0.0;
            double upKbps = 0.0;

            if (lastBytesIn > 0 && currentBytes[0] >= lastBytesIn) {
                downKbps = ((currentBytes[0] - lastBytesIn) / 1024.0) / deltaSec;
            }
            if (lastBytesOut > 0 && currentBytes[1] >= lastBytesOut) {
                upKbps = ((currentBytes[1] - lastBytesOut) / 1024.0) / deltaSec;
            }

            lastBytesIn = currentBytes[0];
            lastBytesOut = currentBytes[1];
            lastTimestamp = now;

            // 2. Extraer conexiones activas del sistema
            List<TrafficConnection> connections = readActiveConnections();

            // 3. Identificar puertos en escucha local para atribuir tráfico entrante con precisión
            Set<Integer> listeningPorts = new HashSet<>();
            int established = 0;
            int listening = 0;
            int timeWait = 0;

            for (TrafficConnection c : connections) {
                String state = c.getState().toUpperCase();
                if (state.contains("LIST")) {
                    listening++;
                    listeningPorts.add(c.getLocalPort());
                } else if (state.contains("ESTAB")) {
                    established++;
                } else if (state.contains("WAIT")) {
                    timeWait++;
                }
            }

            // 4. Agrupar carga y estados por puerto de servicio
            Map<Integer, int[]> portCounters = new HashMap<>(); // [total, established, timeWait]
            Map<Integer, Integer> portMap = new HashMap<>();
            int totalActiveSamples = 0;

            for (TrafficConnection c : connections) {
                String state = c.getState().toUpperCase();
                int localP = c.getLocalPort();
                int remoteP = c.getRemotePort();

                int keyPort;
                if (listeningPorts.contains(localP) || state.contains("LIST")) {
                    keyPort = localP;
                } else if (remoteP > 0 && remoteP < 49152) {
                    keyPort = remoteP;
                } else {
                    keyPort = localP;
                }

                portMap.merge(keyPort, 1, Integer::sum);
                int[] counts = portCounters.computeIfAbsent(keyPort, k -> new int[3]);
                if (!state.contains("LIST")) {
                    counts[0]++; // Conexiones activas / en transición (no solo socket pasivo en escucha)
                    totalActiveSamples++;
                    if (state.contains("ESTAB")) counts[1]++;
                    else if (state.contains("WAIT")) counts[2]++;
                }
            }

            // 5. Construir ranking ordenado de puertos y detectar saturación
            List<PortTrafficSummary> summaries = new ArrayList<>();
            int warnTh = this.warningPortThreshold;
            int critTh = this.criticalPortThreshold;

            for (Map.Entry<Integer, int[]> entry : portCounters.entrySet()) {
                int port = entry.getKey();
                int[] counts = entry.getValue();
                int activeOnPort = counts[0];
                if (activeOnPort == 0) continue; // Omitir puertos pasivos sin conexiones activas

                double concentrationPct = totalActiveSamples > 0
                        ? (activeOnPort * 100.0) / totalActiveSamples
                        : 0.0;

                AlertLevel level = AlertLevel.NORMAL;
                if (activeOnPort >= critTh || counts[2] >= critTh) {
                    level = AlertLevel.CRITICAL;
                } else if (activeOnPort >= warnTh || (concentrationPct >= 45.0 && activeOnPort >= 5)) {
                    level = AlertLevel.WARNING;
                }

                String serviceName = VulnerabilityMatcher.getServiceName(port);
                summaries.add(new PortTrafficSummary(
                        port, serviceName, activeOnPort, counts[1], counts[2], concentrationPct, level
                ));

                // Notificar al motor de amenazas con enfriamiento (cooldown 10s por puerto)
                if (level != AlertLevel.NORMAL) {
                    long lastAlert = portAlertCooldown.getOrDefault(port, 0L);
                    if ((now - lastAlert) >= 10_000L) {
                        portAlertCooldown.put(port, now);
                        ThreatDetectorService.getInstance().evaluatePortSaturation(
                                port, serviceName, activeOnPort, counts[2], concentrationPct, level
                        );
                    }
                }
            }

            summaries.sort(Comparator.comparingInt(PortTrafficSummary::getTotalConnections).reversed());

            if ((now - lastBurstAlertTime) >= 10_000L && (established > 30 || downKbps > 2000.0 || upKbps > 2000.0)) {
                lastBurstAlertTime = now;
                ThreatDetectorService.getInstance().evaluateTrafficBurst("Interfaz Local", established, Math.max(downKbps, upKbps));
            }

            TrafficSnapshot snapshot = new TrafficSnapshot(
                    now,
                    connections,
                    downKbps,
                    upKbps,
                    established,
                    listening,
                    timeWait,
                    portMap,
                    summaries
            );

            if (onTrafficUpdate != null) {
                onTrafficUpdate.accept(snapshot);
            }

        } catch (Exception ex) {
            if (onError != null && running.get()) {
                onError.accept(ex);
            }
        }
    }

    /**
     * Obtiene las conexiones de red TCP activas parseando la tabla de estados del sistema operativo.
     */
    private List<TrafficConnection> readActiveConnections() {
        List<TrafficConnection> list = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        String cmd = isWindows ? "netstat -ano -p tcp" : "netstat -ant";

        try {
            Process process = new ProcessBuilder(cmd.split(" ")).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("Proto") || line.startsWith("Active")) continue;

                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 4) {
                        String proto = tokens[0].toUpperCase();
                        if (!proto.contains("TCP")) continue;

                        String local = tokens[1];
                        String remote = tokens[2];
                        String state = (tokens.length >= 4) ? tokens[3] : "UNKNOWN";
                        long pid = 0;

                        if (isWindows && tokens.length >= 5) {
                            try {
                                pid = Long.parseLong(tokens[4]);
                            } catch (NumberFormatException ignored) {}
                        }

                        // Parsear IP y Puerto Local
                        String[] localParts = splitAddressPort(local);
                        String[] remoteParts = splitAddressPort(remote);

                        String localIp = localParts[0];
                        int localPort = parseIntSafe(localParts[1]);
                        String remoteIp = remoteParts[0];
                        int remotePort = parseIntSafe(remoteParts[1]);

                        final long finalPid = pid;
                        String procName = resolveProcessName(finalPid);

                        String key = proto + ":" + local + "->" + remote;
                        TrafficConnection conn = connectionCache.computeIfAbsent(key, k ->
                            new TrafficConnection(proto, localIp, localPort, remoteIp, remotePort, state, finalPid, procName)
                        );
                        conn.setState(state);
                        conn.incrementPackets();

                        list.add(conn);
                    }
                }
            }
            process.waitFor(1, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        return list;
    }

    /**
     * Consulta las estadísticas de bytes acumulados del adaptador de red (Bytes In / Bytes Out).
     */
    private long[] readInterfaceBytes() {
        long bytesIn = 0;
        long bytesOut = 0;
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        if (isWindows) {
            try {
                Process p = new ProcessBuilder("netstat", "-e").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.toLowerCase().startsWith("bytes")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 3) {
                                bytesIn = Long.parseLong(parts[1]);
                                bytesOut = Long.parseLong(parts[2]);
                                break;
                            }
                        }
                    }
                }
                p.waitFor(500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }
        }

        return new long[]{bytesIn, bytesOut};
    }

    private String[] splitAddressPort(String addrPort) {
        int lastColon = addrPort.lastIndexOf(':');
        if (lastColon > 0) {
            return new String[]{addrPort.substring(0, lastColon), addrPort.substring(lastColon + 1)};
        }
        return new String[]{addrPort, "0"};
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private String resolveProcessName(long pid) {
        if (pid <= 0) return "Sistema";
        try {
            return ProcessHandle.of(pid)
                    .flatMap(h -> h.info().command())
                    .map(cmd -> {
                        int lastSlash = Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\'));
                        return (lastSlash >= 0) ? cmd.substring(lastSlash + 1) : cmd;
                    })
                    .orElse("PID:" + pid);
        } catch (Exception e) {
            return "PID:" + pid;
        }
    }
}
