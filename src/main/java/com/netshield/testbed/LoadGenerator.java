package com.netshield.testbed;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h2>Generador de Carga Concurrente para Demostración en Laboratorio</h2>
 * <p>
 * Genera conexiones concurrentes continuas contra un host y puerto objetivo
 * para simular saturación de conexiones o congestión TCP controlada.
 * </p>
 */
public class LoadGenerator {

    private final String targetHost;
    private final int targetPort;
    private final int concurrentWorkers;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService pool;

    public LoadGenerator(String targetHost, int targetPort, int concurrentWorkers) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.concurrentWorkers = concurrentWorkers;
    }

    public void start() {
        running.set(true);
        pool = Executors.newFixedThreadPool(concurrentWorkers, r -> {
            Thread t = new Thread(r, "StandaloneLoadGen-Worker");
            t.setDaemon(true);
            return t;
        });

        for (int i = 0; i < concurrentWorkers; i++) {
            pool.submit(() -> {
                while (running.get()) {
                    try (Socket socket = new Socket()) {
                        socket.setTcpNoDelay(true);
                        socket.setReuseAddress(true);
                        socket.connect(new InetSocketAddress(targetHost, targetPort), 300);
                        Thread.sleep(25);
                    } catch (Exception ignored) {
                        try { Thread.sleep(50); } catch (InterruptedException ignored2) {}
                    }
                }
            });
        }
        System.out.printf("[LoadGenerator] Generando estrés en %s:%d con %d hilos...%n", targetHost, targetPort, concurrentWorkers);
    }

    public void stop() {
        running.set(false);
        if (pool != null) {
            pool.shutdownNow();
        }
        System.out.println("[LoadGenerator] Generador de carga detenido.");
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        int workers = args.length > 2 ? Integer.parseInt(args[2]) : 20;

        LoadGenerator gen = new LoadGenerator(host, port, workers);
        gen.start();

        System.out.println("Presione ENTER para detener la generación de carga...");
        System.in.read();
        gen.stop();
    }
}
