package com.netshield.service;

import com.netshield.model.AlertLevel;
import com.netshield.model.ScanResult;
import com.netshield.model.ThreatAlert;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * <h2>Motor Central de Detección de Amenazas y Anomalías (NetShield 2.0)</h2>
 * <p>
 * Analiza en tiempo real los eventos recopilados por el Escáner de Puertos,
 * el Monitor de Tráfico y el Laboratorio de Pruebas. Clasifica las alertas
 * en niveles NORMAL, ADVERTENCIA y CRÍTICO con marcas de tiempo y recomendaciones.
 * </p>
 */
public class ThreatDetectorService {

    private static final ThreatDetectorService INSTANCE = new ThreatDetectorService();

    private final ObservableList<ThreatAlert> alerts = FXCollections.observableArrayList();
    private final AtomicInteger alertCounter = new AtomicInteger(100);
    private Consumer<ThreatAlert> onAlertListener;

    private ThreatDetectorService() {}

    public static ThreatDetectorService getInstance() {
        return INSTANCE;
    }

    public void setOnAlertListener(Consumer<ThreatAlert> listener) {
        this.onAlertListener = listener;
    }

    public ObservableList<ThreatAlert> getAlerts() {
        return alerts;
    }

    /**
     * Registra una alerta de seguridad de forma segura para subprocesos.
     */
    public synchronized void recordAlert(String sourceIp, int port, String threatType,
                                        AlertLevel severity, String description, String remediation) {
        String id = "ALT-" + alertCounter.incrementAndGet();
        ThreatAlert alert = new ThreatAlert(
                id,
                LocalDateTime.now(),
                sourceIp,
                port,
                threatType,
                severity,
                description,
                remediation
        );

        Platform.runLater(() -> {
            alerts.add(0, alert); // Inserta al inicio para que las más recientes aparezcan arriba
            if (alerts.size() > 500) {
                alerts.remove(alerts.size() - 1);
            }
        });

        if (onAlertListener != null) {
            onAlertListener.accept(alert);
        }
    }

    /**
     * Evalúa anomalías estadísticas a partir de un resultado de escaneo TCP.
     */
    public void evaluateScanResult(ScanResult result, double baselineEma) {
        if (result.getAlertLevel() == AlertLevel.CRITICAL) {
            recordAlert(
                    result.getIpAddress(),
                    result.getPort(),
                    "Pico Crítico de Latencia & Servicio Vulnerable",
                    AlertLevel.CRITICAL,
                    String.format("Puerto %d (%s) exhibe RTT anómalo (%.3f ms, EMA: %.2f ms) y está catalogado como servicio de alto riesgo.",
                            result.getPort(), result.getServiceName(), result.getRttMillis(), baselineEma),
                    result.getRemediation()
            );
        } else if (result.getAlertLevel() == AlertLevel.WARNING) {
            recordAlert(
                    result.getIpAddress(),
                    result.getPort(),
                    "Desviación de Latencia (Jitter)",
                    AlertLevel.WARNING,
                    String.format("Latencia de %.3f ms sobrepasa la línea base EMA (%.2f ms) con delta de %+.3f ms.",
                            result.getRttMillis(), baselineEma, result.getJitterMillis()),
                    "Monitorear la estabilidad del enlace y descartar saturación en el socket."
            );
        }
    }

    /**
     * Evalúa ráfagas inusuales de conexiones activas detectadas por el monitor.
     */
    public void evaluateTrafficBurst(String target, int activeCount, double rateKbps) {
        if (activeCount > 60 || rateKbps > 5000.0) {
            recordAlert(
                    target,
                    0,
                    "Ráfaga Súbita de Tráfico (Posible Sobrecarga)",
                    AlertLevel.CRITICAL,
                    String.format("Detectadas %d conexiones activas concurrentes con tasa de transferencia de %.1f KB/s.",
                            activeCount, rateKbps),
                    "Activar limitador de tasa (rate-limiting) en cortafuegos y auditar procesos locales."
            );
        } else if (activeCount > 30 || rateKbps > 2000.0) {
            recordAlert(
                    target,
                    0,
                    "Incremento Atípico de Conexiones",
                    AlertLevel.WARNING,
                    String.format("Volumen de conexiones activas elevado (%d sockets simultáneos).", activeCount),
                    "Observar el historial de conexiones para descartar barrido o prueba de carga."
            );
        }
    }

    /**
     * Evalúa concentración anormal o saturación de conexiones en un puerto específico.
     */
    public void evaluatePortSaturation(int port, String serviceName, int totalConnections,
                                       int timeWaitCount, double concentrationPct, AlertLevel level) {
        if (level == AlertLevel.NORMAL) return;

        String title = (level == AlertLevel.CRITICAL)
                ? "Saturación Crítica en Puerto"
                : "Alta Concentración de Tráfico en Puerto";

        String desc = String.format(
                "Puerto %d (%s) concentra %d conexiones (%.1f%% del tráfico activo, TIME_WAIT: %d).",
                port, serviceName, totalConnections, concentrationPct, timeWaitCount
        );

        String action = String.format(
                "Verificar qué proceso atiende el puerto %d, revisar límites de sockets/backlog y aplicar control de tasa si es inesperado.",
                port
        );

        recordAlert("Host Local", port, title, level, desc, action);
    }

    public void clearAlerts() {
        Platform.runLater(alerts::clear);
    }
}
