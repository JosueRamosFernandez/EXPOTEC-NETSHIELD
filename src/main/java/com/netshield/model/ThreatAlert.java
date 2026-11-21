package com.netshield.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo inmutable que representa un incidente de seguridad o anomalía
 * detectada por el motor de análisis de NetShield 2.0.
 */
public record ThreatAlert(
        String id,
        LocalDateTime timestamp,
        String sourceIp,
        int targetPort,
        String threatType,
        AlertLevel severity,
        String description,
        String recommendedAction
) {
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
