package com.netshield.service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * <h2>Motor de Banner Grabbing & Fingerprinting de Servicios</h2>
 * <p>
 * Realiza sondeos no intrusivos a nivel de aplicación sobre puertos abiertos
 * para extraer cabeceras de servidor (HTTP), identificadores de versión (SSH),
 * o mensajes de bienvenida (FTP, SMTP, Telnet).
 * </p>
 */
public class BannerGrabber {

    private static final int BANNER_TIMEOUT_MS = 250;

    /**
     * Intenta extraer el banner o firma de versión de un puerto abierto.
     *
     * @param host Dirección IP o hostname
     * @param port Puerto TCP
     * @return Firma identificada o "N/A"
     */
    public static String grabBanner(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(BANNER_TIMEOUT_MS);
            socket.connect(new InetSocketAddress(host, port), BANNER_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Si es un servicio web comúnmente conocido, enviamos una solicitud HTTP mínima
            if (isHttpPort(port)) {
                String request = "HEAD / HTTP/1.0\r\nHost: " + host + "\r\nUser-Agent: NetShield/1.2\r\n\r\n";
                out.write(request.getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            long start = System.currentTimeMillis();

            while ((System.currentTimeMillis() - start) < BANNER_TIMEOUT_MS && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Si encontramos cabecera Server en respuesta HTTP
                if (line.toLowerCase().startsWith("server:")) {
                    return line.substring(7).trim();
                }

                // Si es un banner directo de SSH, FTP, SMTP o similar
                if (line.startsWith("SSH-") || line.startsWith("220") || line.startsWith("+OK") || line.startsWith("* OK")) {
                    return cleanBanner(line);
                }

                // Si es la línea de estado HTTP (ej. HTTP/1.1 200 OK)
                if (line.startsWith("HTTP/")) {
                    return line;
                }
            }
        } catch (Exception ignored) {
        }
        return "N/A";
    }

    private static boolean isHttpPort(int port) {
        return port == 80 || port == 8080 || port == 8000 || port == 8888 || port == 3000 || port == 5000;
    }

    private static String cleanBanner(String raw) {
        String clean = raw.replaceAll("[\\r\\n\\t]", " ").trim();
        if (clean.length() > 60) {
            clean = clean.substring(0, 57) + "...";
        }
        return clean;
    }
}
