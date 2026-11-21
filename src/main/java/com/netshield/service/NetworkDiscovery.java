package com.netshield.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;

/**
 * <h2>Servicio de Descubrimiento de Red Local</h2>
 * <p>
 * Permite detectar las interfaces de red activas de la máquina y realizar
 * sondeos ICMP Ping concurrentes para identificar hosts activos en la subred /24.
 * </p>
 */
public class NetworkDiscovery {

    private static final int PING_TIMEOUT_MS = 500;
    private static final int MAX_THREADS = 64;

    /**
     * Obtiene las direcciones IPv4 asignadas a las interfaces de red físicas y virtuales activas.
     *
     * @return Lista de IPs locales (excluyendo loopback 127.0.0.1 salvo fallback).
     */
    public static List<String> getLocalIPv4Addresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                ni.getInetAddresses().asIterator().forEachRemaining(addr -> {
                    String host = addr.getHostAddress();
                    // Filtrar solo IPv4 y no loopback
                    if (host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$") && !host.startsWith("127.")) {
                        addresses.add(host);
                    }
                });
            }
        } catch (Exception ignored) {
        }

        if (addresses.isEmpty()) {
            addresses.add("127.0.0.1");
        }
        return addresses;
    }

    /**
     * Extrae el prefijo de subred clase C (/24).
     * Ejemplo: "192.168.1.45" -> "192.168.1"
     */
    public static String getSubnetPrefix(String ip) {
        int lastDot = ip.lastIndexOf('.');
        return (lastDot > 0) ? ip.substring(0, lastDot) : ip;
    }

    /**
     * Escanea concurrentemente los 254 hosts de la subred /24 usando ICMP isReachable.
     *
     * @param subnetPrefix Prefijo de tres octetos (ej. "192.168.1")
     * @return Lista ordenada de IPs activas descubiertas
     */
    public static List<String> discoverActiveHosts(String subnetPrefix) {
        List<String> activeHosts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= 254; i++) {
            final String targetIp = subnetPrefix + "." + i;
            futures.add(pool.submit(() -> {
                try {
                    InetAddress addr = InetAddress.getByName(targetIp);
                    if (addr.isReachable(PING_TIMEOUT_MS)) {
                        activeHosts.add(targetIp);
                    }
                } catch (IOException ignored) {
                }
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get(PING_TIMEOUT_MS + 250, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }
        }

        pool.shutdown();
        Collections.sort(activeHosts);
        return activeHosts;
    }
}
