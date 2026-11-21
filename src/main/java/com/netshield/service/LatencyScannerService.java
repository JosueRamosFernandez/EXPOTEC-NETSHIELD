package com.netshield.service;

import com.netshield.model.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <h2>Motor Principal de Escaneo Concurrente de Alto Rendimiento</h2>
 * <p>
 * Diseñado para soportar desde escaneos específicos hasta el rango completo de
 * 65,535 puertos TCP. Emplea un pool de hilos elástico con CompletionStage no bloqueante,
 * medición de latencia RTT con precisión de nanosegundos, banner grabbing y
 * detección de anomalías con EMA y telemetría discriminada (abiertos, cerrados y filtrados).
 * </p>
 */
public class LatencyScannerService {

    @FunctionalInterface
    public interface DetailedProgressCallback {
        void onProgress(int scanned, int total, int open, int closed, int filtered, double ema, double percent);
    }

    // ── Callbacks de integración con UI ─────────────────────────────────
    private BiConsumer<ScanResult, LatencyBaseline> onResult;
    private Consumer<Double> onProgress;
    private DetailedProgressCallback onDetailedProgress;
    private Consumer<ScanReport> onComplete;
    private Consumer<Exception> onError;

    // ── Estado interno ──────────────────────────────────────────────────
    private volatile boolean cancelled = false;
    private ExecutorService  executor;
    private LatencyBaseline  baseline;
    private ScanReport       report;
    private long             scanStartTime;

    public void setOnResult(BiConsumer<ScanResult, LatencyBaseline> cb)   { this.onResult   = cb; }
    public void setOnProgress(Consumer<Double> cb)                        { this.onProgress  = cb; }
    public void setOnDetailedProgress(DetailedProgressCallback cb)        { this.onDetailedProgress = cb; }
    public void setOnComplete(Consumer<ScanReport> cb)                    { this.onComplete  = cb; }
    public void setOnError(Consumer<Exception> cb)                        { this.onError     = cb; }

    public void startScan(HostScanConfig config) {
        cancelled = false;
        baseline  = new LatencyBaseline(20, config.anomalyThresholdMs());
        report    = new ScanReport(config, baseline);
        scanStartTime = System.currentTimeMillis();

        // Si se van a evaluar más de 5000 puertos (ej. 65,535), dimensionamos el pool a 150 hilos daemon
        int poolSize = (config.totalPorts() > 5000)
                ? Math.max(config.maxConcurrency(), 150)
                : config.maxConcurrency();

        executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "NetShield-Worker");
            t.setDaemon(true);
            return t;
        });

        CompletableFuture.runAsync(() -> orchestrateScan(config), executor)
            .exceptionally(ex -> {
                if (onError != null && !cancelled) {
                    onError.accept(ex instanceof Exception e ? e : new RuntimeException(ex));
                }
                return null;
            });
    }

    public void cancel() {
        cancelled = true;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public boolean isRunning() {
        return executor != null && !executor.isTerminated();
    }

    public LatencyBaseline getBaseline() {
        return baseline;
    }

    public ScanReport getReport() {
        return report;
    }

    /**
     * Orquesta el escaneo sin bloquear secuencialmente. Cada puerto que responde
     * procesa su resultado y actualiza la UI de manera instantánea.
     */
    private void orchestrateScan(HostScanConfig config) {
        int totalPorts = config.totalPorts();
        AtomicInteger completed = new AtomicInteger(0);

        for (int port = config.startPort(); port <= config.endPort() && !cancelled; port++) {
            final int currentPort = port;

            CompletableFuture.supplyAsync(
                () -> probePort(config.targetIp(), currentPort, config.timeoutMs()),
                executor
            ).thenAccept(result -> {
                if (cancelled) return;

                processResult(result, config);

                int done = completed.incrementAndGet();

                // Notificación adaptativa por lotes (evita saturar el event loop en escaneos masivos de 65k puertos)
                if (totalPorts <= 500 || done % 30 == 0 || done == totalPorts) {
                    double progressRatio = (double) done / totalPorts;
                    if (onProgress != null) {
                        onProgress.accept(progressRatio);
                    }
                    if (onDetailedProgress != null) {
                        onDetailedProgress.onProgress(done, totalPorts,
                                report.getOpenPorts(), report.getClosedPorts(), report.getFilteredPorts(),
                                baseline.getEma(), progressRatio);
                    }
                }

                if (done == totalPorts && !cancelled) {
                    finalizeScan();
                }
            }).exceptionally(ex -> {
                int done = completed.incrementAndGet();
                if (done == totalPorts && !cancelled) {
                    finalizeScan();
                }
                return null;
            });
        }
    }

    private void finalizeScan() {
        double elapsed = (System.currentTimeMillis() - scanStartTime) / 1000.0;
        report.setScanDurationSeconds(elapsed);
        shutdownExecutor();
        if (onComplete != null) {
            onComplete.accept(report);
        }
    }

    private void shutdownExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private ScanResult probePort(String ip, int port, int timeout) {
        PortStatus status;
        double rttMs = 0.0;
        String serviceName = VulnerabilityMatcher.getServiceName(port);
        String banner = "N/A";

        try (Socket socket = new Socket()) {
            socket.setReuseAddress(true);
            socket.setTcpNoDelay(true);
            InetSocketAddress addr = new InetSocketAddress(ip, port);

            long t0 = System.nanoTime();
            socket.connect(addr, timeout);
            long t1 = System.nanoTime();

            rttMs  = (t1 - t0) / 1_000_000.0;
            status = PortStatus.OPEN;

        } catch (java.net.ConnectException e) {
            // El host remoto respondió explícitamente con un paquete TCP RST (Puerto Cerrado)
            status = PortStatus.CLOSED;
        } catch (java.net.SocketTimeoutException e) {
            // No hubo respuesta dentro del timeout: paquete descartado por Firewall o ruta (Puerto Filtrado)
            status = PortStatus.FILTERED;
        } catch (IOException e) {
            // Error de red, paquete inalcanzable o filtrado
            status = PortStatus.FILTERED;
        }

        if (status == PortStatus.OPEN) {
            banner = BannerGrabber.grabBanner(ip, port);
        }

        return new ScanResult(ip, port, status, serviceName, rttMs,
                              0.0, AlertLevel.NORMAL, "", banner);
    }

    private synchronized void processResult(ScanResult result, HostScanConfig config) {
        if (result.getStatus() == PortStatus.OPEN) {
            double rtt = result.getRttMillis();

            baseline.addSample(rtt);

            double jitter = baseline.computeJitter(rtt);
            result.setJitterMillis(jitter);

            boolean isAnomaly  = baseline.isAnomaly(rtt);
            boolean isHighRisk = VulnerabilityMatcher.isHighRisk(result.getPort());

            if (isAnomaly && isHighRisk) {
                result.setAlertLevel(AlertLevel.CRITICAL);
            } else if (isAnomaly || isHighRisk) {
                result.setAlertLevel(AlertLevel.WARNING);
            } else {
                result.setAlertLevel(AlertLevel.NORMAL);
            }

            String remediation = VulnerabilityMatcher.getRemediation(result.getPort());
            result.setRemediation(remediation);

            VulnerabilityMatcher.match(result.getPort())
                .ifPresent(report::addVulnerability);
        }

        report.addResult(result);

        if (onResult != null) {
            onResult.accept(result, baseline);
        }
    }
}
