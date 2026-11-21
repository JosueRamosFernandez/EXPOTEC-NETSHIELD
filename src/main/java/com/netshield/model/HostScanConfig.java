package com.netshield.model;

/**
 * Configuración inmutable de un trabajo de escaneo.
 * <p>
 * Encapsula todos los parámetros que el usuario selecciona en el Dashboard
 * antes de lanzar el escaneo, con soporte para el rango completo de puertos (1 - 65,535).
 * </p>
 *
 * @param targetIp          IP o hostname del objetivo (e.g. "192.168.1.1").
 * @param startPort         Puerto TCP inicial del rango (inclusive, 1 a 65535).
 * @param endPort           Puerto TCP final del rango (inclusive, 1 a 65535).
 * @param timeoutMs         Timeout de conexión por puerto en milisegundos.
 * @param maxConcurrency    Hilos simultáneos del pool de escaneo.
 * @param anomalyThresholdMs Umbral delta sobre la línea base para disparar alerta (default 5.0 ms).
 */
public record HostScanConfig(
        String targetIp,
        int    startPort,
        int    endPort,
        int    timeoutMs,
        int    maxConcurrency,
        double anomalyThresholdMs
) {
    public static final int    DEFAULT_TIMEOUT_MS        = 250;
    public static final int    DEFAULT_MAX_CONCURRENCY   = 100;
    public static final double DEFAULT_ANOMALY_THRESHOLD = 5.0;

    public static final int    MIN_PORT                  = 1;
    public static final int    MAX_PORT                  = 65535;

    public HostScanConfig(String targetIp, int startPort, int endPort) {
        this(targetIp, startPort, endPort,
             DEFAULT_TIMEOUT_MS,
             (endPort - startPort + 1 > 5000) ? 150 : DEFAULT_MAX_CONCURRENCY,
             DEFAULT_ANOMALY_THRESHOLD);
    }

    /**
     * Fábrica para crear una configuración de Escaneo Completo de Todos los Puertos (1 - 65,535).
     * Optimiza automáticamente la concurrencia a 150 hilos y timeout balanceado.
     */
    public static HostScanConfig fullRangeScan(String targetIp) {
        return new HostScanConfig(targetIp, MIN_PORT, MAX_PORT, 90, 150, DEFAULT_ANOMALY_THRESHOLD);
    }

    public HostScanConfig {
        if (targetIp == null || targetIp.isBlank())
            throw new IllegalArgumentException("targetIp no puede ser nulo o vacío");
        if (startPort < MIN_PORT || startPort > MAX_PORT)
            throw new IllegalArgumentException("startPort fuera de rango [1-65535]: " + startPort);
        if (endPort < startPort || endPort > MAX_PORT)
            throw new IllegalArgumentException("endPort debe ser >= startPort y <= 65535: " + endPort);
        if (timeoutMs < 10)
            throw new IllegalArgumentException("timeoutMs mínimo = 10 ms");
        if (maxConcurrency < 1)
            throw new IllegalArgumentException("maxConcurrency mínimo = 1");
        if (anomalyThresholdMs <= 0)
            throw new IllegalArgumentException("anomalyThresholdMs debe ser > 0");
    }

    /** Total de puertos a evaluar en este trabajo. */
    public int totalPorts() {
        return endPort - startPort + 1;
    }

    /** Indica si esta configuración corresponde al análisis completo de todos los puertos (1-65535). */
    public boolean isFullRangeScan() {
        return startPort == MIN_PORT && endPort == MAX_PORT;
    }
}
