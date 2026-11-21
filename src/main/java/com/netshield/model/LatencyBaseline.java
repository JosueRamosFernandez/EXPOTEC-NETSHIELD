package com.netshield.model;

/**
 * Línea base dinámica de latencia calculada con
 * <b>Media Móvil Exponencial (EMA)</b>.
 * <p>
 * A diferencia de una media aritmética simple, la EMA reacciona rápidamente
 * a cambios recientes pero suaviza el ruido. Esto es ideal para detectar
 * picos de latencia en tiempo real sin generar falsos positivos por ráfagas
 * momentáneas normales.
 *
 * <h3>Fórmula</h3>
 * <pre>
 *   EMA_t  = α · x_t  +  (1 − α) · EMA_{t−1}
 *   α      = 2 / (N + 1)       donde N = ventana de muestras
 * </pre>
 *
 * <h3>Detección de anomalía</h3>
 * Un RTT se considera anómalo si:
 * <pre>
 *   RTT − EMA  ≥  anomalyThresholdMs   (por defecto 5.0 ms)
 * </pre>
 */
public class LatencyBaseline {

    private final double alpha;              // Factor de suavizado EMA
    private final double anomalyThresholdMs; // Delta mínimo para alertar

    private double ema;          // Valor actual de la EMA
    private long   sampleCount;  // Muestras procesadas
    private double minRtt;       // RTT mínimo observado
    private double maxRtt;       // RTT máximo observado
    private double sumRtt;       // Acumulador para media aritmética

    // ── Constructor ─────────────────────────────────────────────────────
    /**
     * @param windowSize         Número conceptual de muestras para la ventana EMA (≥ 2).
     * @param anomalyThresholdMs Milisegundos de delta sobre la EMA para considerar anomalía.
     */
    public LatencyBaseline(int windowSize, double anomalyThresholdMs) {
        if (windowSize < 2)
            throw new IllegalArgumentException("windowSize debe ser >= 2");
        this.alpha = 2.0 / (windowSize + 1);
        this.anomalyThresholdMs = anomalyThresholdMs;
        this.ema = -1;
        this.sampleCount = 0;
        this.minRtt = Double.MAX_VALUE;
        this.maxRtt = Double.MIN_VALUE;
        this.sumRtt = 0;
    }

    /** Constructor con ventana de 20 muestras y umbral de 5.0 ms. */
    public LatencyBaseline() {
        this(20, HostScanConfig.DEFAULT_ANOMALY_THRESHOLD);
    }

    // ── Actualizar con nueva muestra ────────────────────────────────────
    /**
     * Registra una nueva medición de RTT y recalcula la EMA.
     *
     * @param rttMs RTT en milisegundos.
     */
    public synchronized void addSample(double rttMs) {
        sampleCount++;
        sumRtt += rttMs;
        if (rttMs < minRtt) minRtt = rttMs;
        if (rttMs > maxRtt) maxRtt = rttMs;

        if (ema < 0) {
            // Primera muestra: inicializar EMA con el primer valor
            ema = rttMs;
        } else {
            ema = alpha * rttMs + (1.0 - alpha) * ema;
        }
    }

    // ── Consultas ───────────────────────────────────────────────────────
    /**
     * ¿Es el RTT dado anómalo respecto a la línea base actual?
     *
     * @param rttMs RTT medido en milisegundos.
     * @return {@code true} si el delta supera el umbral configurado.
     */
    public synchronized boolean isAnomaly(double rttMs) {
        if (sampleCount < 2) return false;   // Sin baseline suficiente
        return (rttMs - ema) >= anomalyThresholdMs;
    }

    /**
     * Calcula el jitter (desviación) de un RTT respecto a la EMA actual.
     *
     * @param rttMs RTT medido en milisegundos.
     * @return Delta en milisegundos (puede ser negativo si es más rápido).
     */
    public synchronized double computeJitter(double rttMs) {
        if (sampleCount == 0) return 0.0;
        return rttMs - ema;
    }

    // ── Getters ─────────────────────────────────────────────────────────
    public synchronized double getEma()             { return ema;                          }
    public synchronized double getMean()            { return sampleCount > 0 ? sumRtt / sampleCount : 0; }
    public synchronized double getMinRtt()          { return minRtt;                       }
    public synchronized double getMaxRtt()          { return maxRtt;                       }
    public synchronized long   getSampleCount()     { return sampleCount;                  }
    public synchronized double getAnomalyThreshold(){ return anomalyThresholdMs;           }

    @Override
    public synchronized String toString() {
        return String.format(
                "Baseline[EMA=%.3f ms | Mean=%.3f ms | Min=%.3f | Max=%.3f | Samples=%d | Threshold=%.1f ms]",
                ema, getMean(), minRtt, maxRtt, sampleCount, anomalyThresholdMs);
    }
}
