package com.netshield.testbed;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h2>Módulo de Laboratorio Aislado: Servidor de Pruebas</h2>
 * <p>
 * Abre puertos locales controlados con pool de hilos acotado para simular
 * servicios reales (HTTP 8080, 8081, o anómalo 8082 con retardo configurable).
 * </p>
 */
public class TestServer {

    private final List<ServerSocket> servers = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(32, r -> {
        Thread t = new Thread(r, "StandaloneTestServer-Worker");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean running = false;

    /**
     * Inicia un listener en un puerto dado con un retardo opcional de respuesta.
     */
    public void startListener(int port, int artificialDelayMs) throws IOException {
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress("0.0.0.0", port), 100);
        servers.add(server);
        running = true;

        Thread listenerThread = new Thread(() -> {
            System.out.printf("[TestServer] Escuchando en 0.0.0.0:%d (retardo artificial: %d ms)%n", port, artificialDelayMs);
            while (running && !server.isClosed()) {
                try {
                    Socket client = server.accept();
                    client.setTcpNoDelay(true);
                    client.setSoTimeout(300);

                    threadPool.submit(() -> {
                        try (client) {
                            if (artificialDelayMs > 0) {
                                Thread.sleep(artificialDelayMs);
                            }
                            byte[] response = "HTTP/1.0 200 OK\r\nServer: NetShield-Testbed/1.0\r\n\r\nOK"
                                    .getBytes(StandardCharsets.US_ASCII);
                            client.getOutputStream().write(response);
                            client.getOutputStream().flush();
                        } catch (Exception ignored) {
                        }
                    });
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        }, "Listener-Port-" + port);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        running = false;
        for (ServerSocket s : servers) {
            try { s.close(); } catch (IOException ignored) {}
        }
        threadPool.shutdownNow();
        System.out.println("[TestServer] Servidores de prueba detenidos.");
    }

    public static void main(String[] args) throws Exception {
        TestServer testServer = new TestServer();
        testServer.startListener(8080, 0);   // Normal (0ms)
        testServer.startListener(8081, 1);   // Normal (1ms)
        testServer.startListener(8082, 18);  // Anómalo (+18ms -> disparará ALERTA WARNING/CRITICAL)

        System.out.println("==================================================");
        System.out.println("  NetShield TestServer ACTIVO para Demostración");
        System.out.println("  - Puerto 8080: RTT Normal (~0.5 ms)");
        System.out.println("  - Puerto 8081: RTT Normal (~1.0 ms)");
        System.out.println("  - Puerto 8082: RTT Anómalo (~18.0 ms - Disparará Alerta)");
        System.out.println("  Presione ENTER para detener el servidor...");
        System.out.println("==================================================");

        System.in.read();
        testServer.stop();
    }
}
