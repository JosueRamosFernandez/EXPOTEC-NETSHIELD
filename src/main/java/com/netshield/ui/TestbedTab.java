package com.netshield.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Tab 6: Laboratorio Local de Resiliencia y Simulación Controlada.
 * <p>
 * Opera exclusivamente contra el servidor de demostración local (127.0.0.1:8080-8082).
 * Incluye inicio automático del servidor local si está detenido, lectura completa
 * de respuesta HTTP para medir latencia real bajo saturación, gráfica en tiempo real
 * de disponibilidad y temporizador automático con parada de emergencia.
 * </p>
 */
public class TestbedTab {

    private final Tab tab;

    // Controles del Servidor de Prueba
    private final Button btnToggleServer = new Button("Iniciar Servidor de Demostración");
    private final Label lblServerStatus = new Label("Estado: DETENIDO");
    private final Slider sliderDelay = new Slider(0, 100, 20);
    private final Label lblDelayVal = new Label("20 ms (Retardo artificial en Puerto 8082)");

    // Controles del Generador de Carga Local (restringido a 127.0.0.1 por seguridad)
    private final TextField txtTargetHost = new TextField("127.0.0.1");
    private final ComboBox<String> cmbLoadPort = new ComboBox<>();
    private final Slider sliderThreads = new Slider(5, 45, 15);
    private final Label lblThreadsVal = new Label("15 hilos");

    // Seguridad: Temporizador de prueba programada
    private final Slider sliderDuration = new Slider(5, 60, 15);
    private final Label lblDurationVal = new Label("15 seg");
    private final ProgressBar progressCountdown = new ProgressBar(0);
    private final Label lblCountdown = new Label("Duración programada: Inactivo");

    private final Button btnToggleLoad = new Button("Iniciar Prueba de Carga Local");
    private final Label lblLoadStatus = new Label("Generador: INACTIVO");
    private final Button btnEmergencyStop = new Button("Parada de Emergencia");
    private final Button btnSyncWithDashboard = new Button("Configurar Dashboard con este Laboratorio");

    // Métricas de disponibilidad del servidor bajo carga
    private final Label lblSuccessCount = new Label("0");
    private final Label lblFailedCount = new Label("0");
    private final Label lblAvgRespTime = new Label("-- ms");
    private final Label lblJvmHeap = new Label("-- MB");

    // Gráfica de disponibilidad en vivo
    private final XYChart.Series<Number, Number> successRateSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> failedRateSeries = new XYChart.Series<>();
    private int chartTick = 0;

    // Consola de eventos
    private final TextArea txtLog = new TextArea();

    // Servidores internos
    private final List<ServerSocket> serverSockets = new ArrayList<>();
    private ExecutorService serverWorkerPool;
    private ScheduledExecutorService statsScheduler;
    private volatile boolean serverRunning = false;
    private volatile int currentDelay = 20;
    private final AtomicLong totalConnectionsServed = new AtomicLong(0);

    // Generador interno
    private ExecutorService loadPool;
    private ScheduledExecutorService countdownScheduler;
    private final AtomicBoolean loadRunning = new AtomicBoolean(false);
    private final AtomicLong totalLoadSuccess = new AtomicLong(0);
    private final AtomicLong totalLoadFailed = new AtomicLong(0);
    private final AtomicLong totalResponseTimeSum = new AtomicLong(0);
    private final AtomicReference<String> lastErrorReason = new AtomicReference<>("");

    private BiConsumer<String, String> onConfigureDashboard;
    private VBox rootContainer;

    public TestbedTab() {
        this.tab = new Tab("Laboratorio Local");
        this.tab.setClosable(false);

        buildUI();
    }

    private void buildUI() {
        rootContainer = new VBox(10);
        rootContainer.setPadding(new Insets(12));

        // 1. Panel de Control del Servidor de Prueba
        VBox serverCard = new VBox(8);
        serverCard.getStyleClass().add("card");
        serverCard.setPadding(new Insets(10, 14, 10, 14));

        HBox serverHeader = new HBox(12);
        serverHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblTitleServer = new Label("Servidor Local de Demostración (127.0.0.1 — Puertos 8080, 8081 y 8082)");
        lblTitleServer.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #89b4fa;");

        lblServerStatus.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-font-size: 11px;");

        Region spacerServer = new Region();
        HBox.setHgrow(spacerServer, Priority.ALWAYS);

        btnSyncWithDashboard.getStyleClass().add("button-success");
        btnSyncWithDashboard.setGraphic(UiUtil.icon(UiUtil.PATH_REFRESH, "#11111b", 13));
        btnSyncWithDashboard.setOnAction(e -> syncWithDashboard());

        serverHeader.getChildren().addAll(
            UiUtil.icon(UiUtil.PATH_LAB, "#89b4fa", 16),
            lblTitleServer,
            lblServerStatus,
            spacerServer,
            btnSyncWithDashboard
        );

        HBox serverControls = new HBox(15);
        serverControls.setAlignment(Pos.CENTER_LEFT);

        btnToggleServer.setGraphic(UiUtil.icon(UiUtil.PATH_PLAY, "#11111b", 12));
        btnToggleServer.setOnAction(e -> toggleServer());

        sliderDelay.setShowTickLabels(true);
        sliderDelay.setShowTickMarks(true);
        sliderDelay.setMajorTickUnit(25);
        sliderDelay.setBlockIncrement(5);
        sliderDelay.setPrefWidth(220);
        sliderDelay.valueProperty().addListener((obs, oldV, newV) -> {
            currentDelay = newV.intValue();
            lblDelayVal.setText(currentDelay + " ms (Retardo artificial en Puerto 8082)");
        });

        serverControls.getChildren().addAll(
            btnToggleServer,
            new Label("Retardo artificial:"), sliderDelay, lblDelayVal
        );

        serverCard.getChildren().addAll(serverHeader, serverControls);

        // 2. Panel del Generador de Carga Local Controlada
        VBox loadCard = new VBox(8);
        loadCard.getStyleClass().add("card");
        loadCard.setPadding(new Insets(10, 14, 10, 14));

        HBox loadHeader = new HBox(12);
        loadHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblTitleLoad = new Label("Banco de Pruebas de Disponibilidad (Solo Entorno Local 127.0.0.1)");
        lblTitleLoad.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #fab387;");

        lblLoadStatus.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");

        Region spacerLoad = new Region();
        HBox.setHgrow(spacerLoad, Priority.ALWAYS);

        btnEmergencyStop.getStyleClass().add("button-danger");
        btnEmergencyStop.setGraphic(UiUtil.icon(UiUtil.PATH_STOP, "#11111b", 12));
        btnEmergencyStop.setOnAction(e -> emergencyStop());

        loadHeader.getChildren().addAll(
            UiUtil.icon(UiUtil.PATH_BOLT, "#fab387", 16),
            lblTitleLoad,
            lblLoadStatus,
            spacerLoad,
            btnEmergencyStop
        );

        HBox loadControls = new HBox(10);
        loadControls.setAlignment(Pos.CENTER_LEFT);

        txtTargetHost.setEditable(false);
        txtTargetHost.setPrefWidth(95);
        txtTargetHost.setTooltip(new Tooltip("Por seguridad, las pruebas de carga se ejecutan únicamente contra localhost (127.0.0.1)."));

        cmbLoadPort.getItems().addAll(
            "8080 (Servicio Normal)",
            "8081 (Servicio Normal)",
            "8082 (Con Retardo / Saturable)"
        );
        cmbLoadPort.setValue("8080 (Servicio Normal)");
        cmbLoadPort.setPrefWidth(195);

        sliderThreads.setShowTickLabels(true);
        sliderThreads.setShowTickMarks(true);
        sliderThreads.setMajorTickUnit(10);
        sliderThreads.setPrefWidth(145);
        sliderThreads.valueProperty().addListener((obs, oldV, newV) ->
            lblThreadsVal.setText(newV.intValue() + " hilos")
        );

        sliderDuration.setShowTickLabels(true);
        sliderDuration.setShowTickMarks(true);
        sliderDuration.setMajorTickUnit(15);
        sliderDuration.setPrefWidth(135);
        sliderDuration.valueProperty().addListener((obs, oldV, newV) ->
            lblDurationVal.setText(newV.intValue() + " seg")
        );

        btnToggleLoad.getStyleClass().add("button-purple");
        btnToggleLoad.setGraphic(UiUtil.icon(UiUtil.PATH_BOLT, "#11111b", 12));
        btnToggleLoad.setOnAction(e -> toggleLoad());

        Region ctrlSpacer = new Region();
        HBox.setHgrow(ctrlSpacer, Priority.ALWAYS);

        loadControls.getChildren().addAll(
            new Label("Host:"), txtTargetHost,
            new Label("Puerto:"), cmbLoadPort,
            new Label("Hilos:"), sliderThreads, lblThreadsVal,
            new Label("Duración:"), sliderDuration, lblDurationVal,
            ctrlSpacer,
            btnToggleLoad
        );

        HBox countdownRow = new HBox(12);
        countdownRow.setAlignment(Pos.CENTER_LEFT);
        progressCountdown.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressCountdown, Priority.ALWAYS);
        lblCountdown.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");
        countdownRow.getChildren().addAll(progressCountdown, lblCountdown);

        loadCard.getChildren().addAll(loadHeader, loadControls, countdownRow);

        // 3. Tarjetas KPI de Disponibilidad y Estrés del Servidor
        HBox kpiRow = new HBox(10);
        kpiRow.getChildren().addAll(
            UiUtil.metricCard("SOLICITUDES EXITOSAS", lblSuccessCount, "200 OK verificados", "#a6e3a1",
                UiUtil.icon(UiUtil.PATH_CHECK, "#a6e3a1", 13)),
            UiUtil.metricCard("SOLICITUDES FALLIDAS", lblFailedCount, "Rechazos / Timeouts", "#f38ba8",
                UiUtil.icon(UiUtil.PATH_WARNING, "#f38ba8", 13)),
            UiUtil.metricCard("LATENCIA BAJO CARGA", lblAvgRespTime, "Tiempo medio HTTP", "#89b4fa",
                UiUtil.icon(UiUtil.PATH_CHART, "#89b4fa", 13)),
            UiUtil.metricCard("MEMORIA JVM EN USO", lblJvmHeap, "Heap actual del servidor", "#cba6f7",
                UiUtil.icon(UiUtil.PATH_SPEED, "#cba6f7", 13))
        );

        // 4. Gráfica de Disponibilidad + Consola de Telemetría lado a lado
        HBox bottomArea = new HBox(12);
        VBox.setVgrow(bottomArea, Priority.ALWAYS);

        // Gráfica en vivo
        VBox chartCard = new VBox(6);
        chartCard.getStyleClass().add("card");
        chartCard.setPadding(new Insets(10));
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        chartCard.setPrefWidth(520);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Tiempo (ticks 0.5s)");
        xAxis.setAutoRanging(true);
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Solicitudes / intervalo");
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Disponibilidad del Servidor Local bajo Carga");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        VBox.setVgrow(chart, Priority.ALWAYS);

        successRateSeries.setName("Exitosas (200 OK)");
        failedRateSeries.setName("Fallidas / Timeout");
        chart.getData().addAll(successRateSeries, failedRateSeries);
        chartCard.getChildren().add(chart);

        // Consola de Log
        VBox logCard = new VBox(6);
        logCard.getStyleClass().add("card");
        logCard.setPadding(new Insets(10));
        HBox.setHgrow(logCard, Priority.ALWAYS);
        logCard.setPrefWidth(540);

        HBox logHeader = new HBox(10);
        logHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblLogTitle = new Label("Consola de Telemetría y Diagnóstico");
        lblLogTitle.setStyle("-fx-text-fill: #a6adc8; -fx-font-weight: bold; -fx-font-size: 11px;");

        Region spacerLog = new Region();
        HBox.setHgrow(spacerLog, Priority.ALWAYS);

        Button btnClearLog = new Button("Limpiar");
        btnClearLog.getStyleClass().add("button-secondary");
        btnClearLog.setStyle("-fx-padding: 3 8; -fx-font-size: 10px;");
        btnClearLog.setGraphic(UiUtil.icon(UiUtil.PATH_BROOM, "#89b4fa", 11));
        btnClearLog.setOnAction(e -> txtLog.clear());
        logHeader.getChildren().addAll(lblLogTitle, spacerLog, btnClearLog);

        txtLog.setEditable(false);
        txtLog.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: #a6e3a1;");
        VBox.setVgrow(txtLog, Priority.ALWAYS);

        logCard.getChildren().addAll(logHeader, txtLog);
        bottomArea.getChildren().addAll(chartCard, logCard);

        rootContainer.getChildren().addAll(serverCard, loadCard, kpiRow, bottomArea);
        tab.setContent(rootContainer);

        log("Laboratorio listo. Puede iniciar el Servidor de Demostración o lanzar una prueba local directamente.");
    }

    private void syncWithDashboard() {
        if (!serverRunning) {
            startTestServer();
        }
        if (onConfigureDashboard != null) {
            onConfigureDashboard.accept("127.0.0.1", "8079-8085");
            log("Configuración enviada al Dashboard: IP=127.0.0.1, Puertos=8079 a 8085.");
        }
    }

    private void toggleServer() {
        if (!serverRunning) {
            startTestServer();
        } else {
            stopTestServer();
        }
    }

    private synchronized void startTestServer() {
        if (serverRunning) return;
        try {
            serverSockets.clear();
            serverWorkerPool = Executors.newFixedThreadPool(32, r -> {
                Thread t = new Thread(r, "TestServer-Worker");
                t.setDaemon(true);
                return t;
            });
            serverRunning = true;
            totalConnectionsServed.set(0);

            startPortListener(8080, false);
            startPortListener(8081, false);
            startPortListener(8082, true);

            statsScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Testbed-Stats");
                t.setDaemon(true);
                return t;
            });
            statsScheduler.scheduleAtFixedRate(() -> {
                if (serverRunning) {
                    long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
                    long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
                    long usedMem = totalMem - freeMem;
                    Platform.runLater(() -> lblJvmHeap.setText(usedMem + " MB / " + totalMem + " MB"));
                }
            }, 0, 1, TimeUnit.SECONDS);

            lblServerStatus.setText("Estado: EN EJECUCIÓN (8080 normal, 8081 normal, 8082 retardo)");
            lblServerStatus.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold; -fx-font-size: 11px;");
            btnToggleServer.setText("Detener Servidor");
            btnToggleServer.getStyleClass().clear();
            btnToggleServer.getStyleClass().addAll("button", "button-danger");
            btnToggleServer.setGraphic(UiUtil.icon(UiUtil.PATH_STOP, "#11111b", 12));
            log("Servidor iniciado en 127.0.0.1: Puerto 8080 (0ms), 8081 (0ms), 8082 (+" + currentDelay + "ms retardo).");
        } catch (Exception ex) {
            serverRunning = false;
            log("Error al iniciar servidor local (verifique que los puertos 8080-8082 estén libres): " + ex.getMessage());
        }
    }

    private void startPortListener(int port, boolean hasArtificialDelay) throws IOException {
        ServerSocket s = new ServerSocket();
        s.setReuseAddress(true);
        s.bind(new InetSocketAddress("127.0.0.1", port), 100);
        serverSockets.add(s);

        Thread acceptThread = new Thread(() -> {
            while (serverRunning && !s.isClosed()) {
                try {
                    Socket client = s.accept();
                    client.setTcpNoDelay(true);
                    client.setSoTimeout(1500);

                    serverWorkerPool.submit(() -> {
                        try (client) {
                            totalConnectionsServed.incrementAndGet();

                            if (hasArtificialDelay && currentDelay > 0) {
                                Thread.sleep(currentDelay);
                            }

                            byte[] response = ("HTTP/1.0 200 OK\r\nServer: NetShield-SimulatedService/1.0\r\nContent-Length: 2\r\n\r\nOK")
                                    .getBytes(StandardCharsets.US_ASCII);
                            client.getOutputStream().write(response);
                            client.getOutputStream().flush();
                        } catch (Exception ignored) {
                        }
                    });
                } catch (IOException e) {
                    if (!serverRunning) break;
                }
            }
        }, "Acceptor-Port-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private synchronized void stopTestServer() {
        serverRunning = false;
        for (ServerSocket s : serverSockets) {
            try { s.close(); } catch (Exception ignored) {}
        }
        serverSockets.clear();
        if (serverWorkerPool != null) serverWorkerPool.shutdownNow();
        if (statsScheduler != null) statsScheduler.shutdownNow();

        lblServerStatus.setText("Estado: DETENIDO");
        lblServerStatus.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-font-size: 11px;");
        btnToggleServer.setText("Iniciar Servidor de Demostración");
        btnToggleServer.getStyleClass().clear();
        btnToggleServer.getStyleClass().add("button");
        btnToggleServer.setGraphic(UiUtil.icon(UiUtil.PATH_PLAY, "#11111b", 12));
        log("Servidores de demostración detenidos correctamente.");
    }

    private void toggleLoad() {
        if (!loadRunning.get()) {
            startLoad();
        } else {
            stopLoad();
        }
    }

    private int parseSelectedPort() {
        String val = cmbLoadPort.getValue();
        if (val != null) {
            String digits = val.split("\\s+")[0].trim();
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }

    private void startLoad() {
        try {
            String host = txtTargetHost.getText().trim();
            if (!"127.0.0.1".equals(host) && !"localhost".equalsIgnoreCase(host)) {
                log("Bloqueado por seguridad: El banco de pruebas solo opera sobre 127.0.0.1.");
                return;
            }

            int port = parseSelectedPort();

            // Auto-iniciar el servidor local si el usuario olvidó encenderlo antes de lanzar la prueba
            if (!serverRunning) {
                log("El servidor local estaba detenido. Iniciando servidor de demostración automáticamente...");
                startTestServer();
                if (!serverRunning) {
                    log("No se pudo iniciar el servidor local. Abortando prueba.");
                    return;
                }
            }

            int workers = (int) sliderThreads.getValue();
            int durationSeconds = (int) sliderDuration.getValue();

            loadRunning.set(true);
            totalLoadSuccess.set(0);
            totalLoadFailed.set(0);
            totalResponseTimeSum.set(0);
            lastErrorReason.set("");
            chartTick = 0;

            successRateSeries.getData().clear();
            failedRateSeries.getData().clear();

            lblSuccessCount.setText("0");
            lblFailedCount.setText("0");
            lblAvgRespTime.setText("-- ms");
            progressCountdown.setProgress(0);

            loadPool = Executors.newFixedThreadPool(workers, r -> {
                Thread t = new Thread(r, "LoadGen-Worker");
                t.setDaemon(true);
                return t;
            });

            final int targetPort = port;

            for (int i = 0; i < workers; i++) {
                loadPool.submit(() -> {
                    while (loadRunning.get()) {
                        long t0 = System.nanoTime();
                        try (Socket s = new Socket()) {
                            s.setTcpNoDelay(true);
                            s.setReuseAddress(true);
                            s.setSoTimeout(1200);
                            s.connect(new InetSocketAddress("127.0.0.1", targetPort), 600);

                            // Leer la respuesta HTTP real del servidor de pruebas para medir latencia de extremo a extremo
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
                            String statusLine = reader.readLine();
                            long t1 = System.nanoTime();

                            if (statusLine != null && statusLine.contains("200 OK")) {
                                long elapsedMs = Math.max(1, (t1 - t0) / 1_000_000L);
                                totalResponseTimeSum.addAndGet(elapsedMs);
                                totalLoadSuccess.incrementAndGet();
                            } else {
                                lastErrorReason.set("Respuesta incompleta o cerrada por servidor");
                                totalLoadFailed.incrementAndGet();
                            }

                            Thread.sleep(25);
                        } catch (Exception ex) {
                            lastErrorReason.set(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                            totalLoadFailed.incrementAndGet();
                            try { Thread.sleep(50); } catch (InterruptedException ignored2) {}
                        }
                    }
                });
            }

            countdownScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Testbed-Countdown");
                t.setDaemon(true);
                return t;
            });

            final long startTime = System.currentTimeMillis();
            final long totalMillis = durationSeconds * 1000L;
            final AtomicLong prevSuccess = new AtomicLong(0);
            final AtomicLong prevFailed = new AtomicLong(0);

            countdownScheduler.scheduleAtFixedRate(() -> {
                if (!loadRunning.get()) return;

                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = Math.max(0, totalMillis - elapsed);
                double progress = Math.min(1.0, (double) elapsed / totalMillis);

                long success = totalLoadSuccess.get();
                long failed = totalLoadFailed.get();
                long deltaSuccess = success - prevSuccess.getAndSet(success);
                long deltaFailed = failed - prevFailed.getAndSet(failed);

                long latencySum = totalResponseTimeSum.get();
                double avgLat = (success > 0) ? (double) latencySum / success : 0.0;
                String errMsg = lastErrorReason.getAndSet("");

                Platform.runLater(() -> {
                    progressCountdown.setProgress(progress);
                    lblCountdown.setText(String.format("Tiempo restante: %ds / %ds", remaining / 1000, durationSeconds));
                    lblSuccessCount.setText(String.valueOf(success));
                    lblFailedCount.setText(String.valueOf(failed));
                    lblAvgRespTime.setText(success > 0 ? String.format("%.2f ms", avgLat) : "-- ms");

                    chartTick++;
                    successRateSeries.getData().add(new XYChart.Data<>(chartTick, deltaSuccess));
                    failedRateSeries.getData().add(new XYChart.Data<>(chartTick, deltaFailed));
                    if (successRateSeries.getData().size() > 30) {
                        successRateSeries.getData().remove(0);
                        failedRateSeries.getData().remove(0);
                    }
                });

                if (!errMsg.isEmpty() && deltaFailed > 0) {
                    log("Diagnóstico de fallos (" + deltaFailed + " en intervalo): " + errMsg);
                }

                if (elapsed >= totalMillis) {
                    Platform.runLater(() -> {
                        log(String.format("Prueba local completada (%d seg). Exitosas: %d | Fallidas: %d | Latencia Media: %.2f ms",
                                durationSeconds, success, failed, avgLat));
                        stopLoad();
                    });
                }
            }, 500, 500, TimeUnit.MILLISECONDS);

            lblLoadStatus.setText("Generador: ACTIVO (" + workers + " hilos contra 127.0.0.1:" + targetPort + ")");
            lblLoadStatus.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold;");
            btnToggleLoad.setText("Detener Prueba");
            btnToggleLoad.getStyleClass().clear();
            btnToggleLoad.getStyleClass().addAll("button", "button-danger");
            btnToggleLoad.setGraphic(UiUtil.icon(UiUtil.PATH_STOP, "#11111b", 12));
            log(String.format("Prueba de carga iniciada contra 127.0.0.1:%d | Hilos: %d | Duración: %d seg. (Tip: abre 'Monitoreo de Tráfico' para ver el puerto saturado)",
                    targetPort, workers, durationSeconds));

        } catch (Exception ex) {
            log("Error al iniciar prueba local: " + ex.getMessage());
        }
    }

    private void stopLoad() {
        loadRunning.set(false);
        if (loadPool != null) loadPool.shutdownNow();
        if (countdownScheduler != null) countdownScheduler.shutdownNow();

        lblLoadStatus.setText("Generador: INACTIVO");
        lblLoadStatus.setStyle("-fx-text-fill: #a6adc8;");
        btnToggleLoad.setText("Iniciar Prueba de Carga Local");
        btnToggleLoad.getStyleClass().clear();
        btnToggleLoad.getStyleClass().addAll("button", "button-purple");
        btnToggleLoad.setGraphic(UiUtil.icon(UiUtil.PATH_BOLT, "#11111b", 12));
        progressCountdown.setProgress(1.0);
        lblCountdown.setText("Prueba finalizada");
    }

    private void emergencyStop() {
        stopLoad();
        stopTestServer();
        log("⛔ PARADA DE EMERGENCIA EJECUTADA: Servidor y Generador detenidos inmediatamente.");
    }

    private void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            txtLog.appendText(String.format("[%s] %s%n", time, msg));
            if (txtLog.getText().length() > 20000) {
                txtLog.setText(txtLog.getText().substring(txtLog.getText().length() - 10000));
            }
        });
    }

    public void setOnConfigureDashboard(BiConsumer<String, String> cb) {
        this.onConfigureDashboard = cb;
    }

    public void shutdown() {
        emergencyStop();
    }

    public Tab getTab() { return tab; }
    public VBox getRoot() { return rootContainer; }
}
