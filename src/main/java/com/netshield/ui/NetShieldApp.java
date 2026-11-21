package com.netshield.ui;

import com.netshield.service.LatencyScannerService;
import com.netshield.service.ThreatDetectorService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada principal de NetShield 2.0 Pro.
 * Incorpora un sistema de navegación animado y fluido (Slide + Fade Transitions),
 * iconos vectoriales SVG con renderizado limpio en Windows, monitoreo de tráfico en vivo,
 * laboratorio DDoS controlado, detección centralizada de amenazas y atajos de teclado globales.
 */
public class NetShieldApp extends Application {

    private final List<Node> views = new ArrayList<>();
    private final Button[] navButtons = new Button[6];
    private int currentIndex = 0;
    private boolean isAnimating = false;
    private StackPane contentViewport;

    @Override
    public void start(Stage primaryStage) {
        LatencyScannerService scannerService = new LatencyScannerService();

        // 1. Banner Superior
        VBox banner = buildBanner();

        // 2. Inicializar los 6 módulos de NetShield 2.0
        DashboardTab dashboardTab = new DashboardTab(scannerService);
        TrafficMonitorTab trafficTab = new TrafficMonitorTab();
        MetricsTab metricsTab = new MetricsTab();
        RemediationTab remediationTab = new RemediationTab();
        NetworkDiscoveryTab discoveryTab = new NetworkDiscoveryTab();
        TestbedTab testbedTab = new TestbedTab();

        // Extraer vistas principales
        views.add(dashboardTab.getRoot());
        views.add(trafficTab.getRoot());
        views.add(metricsTab.getRoot());
        views.add(remediationTab.getRoot());
        views.add(discoveryTab.getRoot());
        views.add(testbedTab.getRoot());

        // 3. Contenedor de Contenido con viewport recortado para transiciones fluidas
        contentViewport = new StackPane();
        contentViewport.setStyle("-fx-background-color: #181825;");
        VBox.setVgrow(contentViewport, Priority.ALWAYS);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(contentViewport.widthProperty());
        clip.heightProperty().bind(contentViewport.heightProperty());
        contentViewport.setClip(clip);

        for (int i = 0; i < views.size(); i++) {
            Node v = views.get(i);
            contentViewport.getChildren().add(v);
            if (i == 0) {
                v.setVisible(true);
                v.setOpacity(1.0);
            } else {
                v.setVisible(false);
                v.setOpacity(0.0);
            }
        }

        // 4. Barra de Navegación Segmentada Animada
        HBox navBar = buildNavigationBar();

        // 5. Cableado interactivo e inteligencia cruzada entre módulos:
        dashboardTab.setOnResultCallback(result -> {
            metricsTab.addScanPoint(result);
            ThreatDetectorService.getInstance().evaluateScanResult(result, scannerService.getBaseline().getEma());
        });
        dashboardTab.setOnScanCompleteCallback(remediationTab::updateReport);

        // Cuando se selecciona un host en Descubrimiento, se envía a Dashboard y se transiciona suavemente
        discoveryTab.setOnHostSelectedForScan(ip -> {
            dashboardTab.setTargetIp(ip);
            selectTab(0);
        });

        // Cuando se sincroniza el Laboratorio con el Dashboard, se configuran IP y puertos y se transiciona
        testbedTab.setOnConfigureDashboard((ip, range) -> {
            dashboardTab.configureTarget(ip, 8079, 8085);
            selectTab(0);
        });

        // 6. Barra de Estado Inferior
        HBox statusBar = buildStatusBar();

        // 7. Ensamble de la estructura principal
        BorderPane root = new BorderPane();
        VBox topHeaderArea = new VBox(banner, navBar);
        root.setTop(topHeaderArea);
        root.setCenter(contentViewport);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1260, 820);

        // Atajos de teclado para cambio rápido de pestañas (Ctrl+1 a Ctrl+6)
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN), () -> selectTab(0));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN), () -> selectTab(1));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN), () -> selectTab(2));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN), () -> selectTab(3));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN), () -> selectTab(4));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT6, KeyCombination.CONTROL_DOWN), () -> selectTab(5));

        // Cargar hoja de estilo CSS
        URL cssResource = getClass().getResource("/css/netshield.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setTitle("NetShield 2.0 Pro — Sistema Avanzado de Análisis, Monitoreo y Evaluación de Redes");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setOnCloseRequest(e -> {
            scannerService.cancel();
            trafficTab.shutdown();
            testbedTab.shutdown();
        });
        primaryStage.show();
    }

    /**
     * Realiza una transición animada fluida (Slide + Fade) entre vistas con aceleración cúbica.
     */
    public void selectTab(int targetIndex) {
        if (targetIndex == currentIndex || targetIndex < 0 || targetIndex >= views.size() || isAnimating) {
            return;
        }

        isAnimating = true;
        int direction = (targetIndex > currentIndex) ? 1 : -1;

        Node oldView = views.get(currentIndex);
        Node newView = views.get(targetIndex);

        // Actualizar estados visuales de los botones de navegación
        if (navButtons[currentIndex] != null) {
            navButtons[currentIndex].getStyleClass().remove("nav-tab-active");
        }
        if (navButtons[targetIndex] != null) {
            navButtons[targetIndex].getStyleClass().add("nav-tab-active");
        }

        // Preparar la vista entrante
        newView.setOpacity(0.0);
        newView.setTranslateX(direction * 45.0);
        newView.setVisible(true);
        newView.toFront();

        // Animación entrante (Slide In + Fade In)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(240), newView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(240), newView);
        slideIn.setFromX(direction * 45.0);
        slideIn.setToX(0.0);
        slideIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

        // Animación saliente (Slide Out + Fade Out)
        FadeTransition fadeOut = new FadeTransition(Duration.millis(170), oldView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(170), oldView);
        slideOut.setFromX(0.0);
        slideOut.setToX(-direction * 35.0);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(fadeIn, slideIn, fadeOut, slideOut);
        transition.setOnFinished(e -> {
            oldView.setVisible(false);
            oldView.setTranslateX(0.0);
            oldView.setOpacity(1.0);
            currentIndex = targetIndex;
            isAnimating = false;
        });
        transition.play();
    }

    private HBox buildNavigationBar() {
        HBox navBar = new HBox(4);
        navBar.getStyleClass().add("nav-bar");
        navBar.setAlignment(Pos.CENTER_LEFT);

        String[] titles = {
            "Dashboard & Escaneo",
            "Monitoreo de Tráfico",
            "Métricas & Latencia",
            "Plan de Remediación & Reportes",
            "Descubrimiento de Red",
            "Laboratorio DDoS"
        };

        String[] icons = {
            UiUtil.PATH_DASHBOARD,
            UiUtil.PATH_SPEED,
            UiUtil.PATH_CHART,
            UiUtil.PATH_CLIPBOARD,
            UiUtil.PATH_NETWORK,
            UiUtil.PATH_LAB
        };

        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            Button btn = new Button(titles[i]);
            btn.getStyleClass().add("nav-tab");
            btn.setGraphic(UiUtil.icon(icons[i], i == 0 ? "#89b4fa" : "#a6adc8", 14));

            if (i == 0) {
                btn.getStyleClass().add("nav-tab-active");
            }

            btn.setOnAction(e -> selectTab(index));
            navButtons[i] = btn;
            navBar.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label shortcutHint = new Label("Atajos: Ctrl+1 a Ctrl+6");
        shortcutHint.setStyle("-fx-text-fill: #585b70; -fx-font-size: 10px; -fx-padding: 0 10 0 0;");

        navBar.getChildren().addAll(spacer, shortcutHint);
        return navBar;
    }

    private VBox buildBanner() {
        VBox banner = new VBox(2);
        banner.getStyleClass().add("app-banner");
        banner.setPadding(new Insets(12, 20, 8, 20));

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        // Logo vectorial con marco redondeado
        StackPane iconBox = new StackPane(UiUtil.icon(UiUtil.PATH_SHIELD, "#89b4fa", 24));
        iconBox.getStyleClass().add("brand-icon-box");

        VBox titleBox = new VBox(1);
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("NetShield");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #89b4fa;");

        Label badgePro = UiUtil.badge("2.0 PRO", "purple");

        titleRow.getChildren().addAll(title, badgePro);

        Label subtitle = new Label("Sistema Avanzado de Análisis, Monitoreo de Tráfico, Detección de Amenazas y Evaluación de Redes");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #a6adc8;");
        titleBox.getChildren().addAll(titleRow, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label liveDot = new Label("● En Línea");
        liveDot.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label badge = new Label("Java " + System.getProperty("java.version") + " (LTS)");
        badge.setStyle("-fx-font-size: 11px; -fx-text-fill: #89b4fa; -fx-background-color: #1e1e2e; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: #313244; -fx-border-radius: 6;");

        row.getChildren().addAll(iconBox, titleBox, spacer, liveDot, badge);
        banner.getChildren().add(row);
        return banner;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(20);
        bar.getStyleClass().add("status-bar");
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setAlignment(Pos.CENTER_LEFT);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Label lblTime = new Label("Sesión Activa: " + ts);
        lblTime.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 10px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblAcademic = new Label("Proyecto Académico — Ingeniería de Sistemas (Redes de Computadoras & Ciberseguridad Avanzada)");
        lblAcademic.setStyle("-fx-text-fill: #585b70; -fx-font-size: 10px;");

        bar.getChildren().addAll(lblTime, spacer, lblAcademic);
        return bar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
