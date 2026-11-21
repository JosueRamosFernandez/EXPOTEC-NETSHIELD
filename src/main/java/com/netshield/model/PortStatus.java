package com.netshield.model;

/**
 * Estado posible de un puerto TCP tras el intento de conexión.
 *
 * <ul>
 *   <li>{@link #OPEN}    — El socket se conectó con éxito (three-way handshake completo).</li>
 *   <li>{@link #CLOSED}  — La conexión fue rechazada activamente (RST).</li>
 *   <li>{@link #FILTERED}— El timeout expiró sin respuesta (firewall/drop).</li>
 * </ul>
 */
public enum PortStatus {
    OPEN,
    CLOSED,
    FILTERED
}
