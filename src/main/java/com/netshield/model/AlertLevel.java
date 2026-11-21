package com.netshield.model;

/**
 * Nivel de alerta asignado a un resultado de escaneo.
 *
 * <ul>
 *   <li>{@link #NORMAL}  — Latencia dentro de la línea base (&lt; 5 ms sobre el baseline).</li>
 *   <li>{@link #WARNING} — Latencia elevada (≥ 5 ms sobre el baseline); posible anomalía.</li>
 *   <li>{@link #CRITICAL}— Puerto crítico expuesto (SMB 445, RDP 3389, etc.) <b>y</b>
 *       latencia anómala, señal de posible exfiltración o compromiso activo.</li>
 * </ul>
 */
public enum AlertLevel {
    NORMAL,
    WARNING,
    CRITICAL
}
