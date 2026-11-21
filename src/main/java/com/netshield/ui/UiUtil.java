package com.netshield.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Utilidades de diseño visual para NetShield UI.
 * Provee iconos vectoriales SVG limpios (independientes del soporte de fuentes/emojis del sistema operativo),
 * badges de estado estilizados y componentes de interfaz modernos.
 */
public final class UiUtil {

    private UiUtil() {}

    // Iconos vectoriales estándar (24x24 viewBox)
    public static final String PATH_SHIELD = "M12 2L4 5v6.09c0 5.05 3.41 9.76 8 10.91 4.59-1.15 8-5.86 8-10.91V5l-8-3zm0 2.18l6 2.25v4.66c0 4.02-2.67 7.79-6 8.82-3.33-1.03-6-4.8-6-8.82V6.43l6-2.25z";
    public static final String PATH_DASHBOARD = "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    public static final String PATH_CHART = "M3.5 18.5l6-6 4 4L22 6.92 20.59 5.5l-7.09 7.08-4-4L2 16.08z";
    public static final String PATH_CLIPBOARD = "M19 3h-4.18C14.4 1.84 13.3 1 12 1s-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    public static final String PATH_NETWORK = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z";
    public static final String PATH_LAB = "M19 19c0 1.1-.9 2-2 2H7c-1.1 0-2-.9-2-2l4.5-9V5H8V3h8v2h-1.5v5L19 19z";
    public static final String PATH_PLAY = "M8 5v14l11-7z";
    public static final String PATH_STOP = "M6 6h12v12H6z";
    public static final String PATH_ROCKET = "M12 2.5s-4 4.5-4 9.5c0 3.5 2 5.5 4 5.5s4-2 4-5.5c0-5-4-9.5-4-9.5zm-5 13l-3 4 4.5-1.5L7 15.5zm10 0l-1.5 2.5 4.5 1.5-3-4z";
    public static final String PATH_SEARCH = "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
    public static final String PATH_REFRESH = "M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    public static final String PATH_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    public static final String PATH_WARNING = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    public static final String PATH_DOWNLOAD = "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z";
    public static final String PATH_FILTER = "M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z";
    public static final String PATH_BOLT = "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C14.9 14.84 12.96 18.24 11 21z";
    public static final String PATH_BROOM = "M19.36 10.04l-7.4-7.4a1.996 1.996 0 0 0-2.83 0l-6.8 6.8c-.78.78-.78 2.05 0 2.83l7.4 7.4c.78.78 2.05.78 2.83 0l6.8-6.8a1.996 1.996 0 0 0 0-2.83zm-11.8 7.12l-5.38-5.38 6.8-6.8 5.38 5.38-6.8 6.8z";
    public static final String PATH_SPEED = "M12 4C7.03 4 3 8.03 3 13c0 3.32 1.8 6.22 4.47 7.78l1.41-1.41C6.72 18.06 5.5 15.68 5.5 13c0-3.59 2.91-6.5 6.5-6.5s6.5 2.91 6.5 6.5c0 2.68-1.22 5.06-3.38 6.37l1.41 1.41C19.2 19.22 21 16.32 21 13c0-4.97-4.03-9-9-9zm-1 6v5.27c-.6.34-1 .98-1 1.73 0 1.1.9 2 2 2s2-.9 2-2c0-.75-.4-1.39-1-1.73V10h-2z";

    /**
     * Crea un nodo de icono vectorial SVG escalado y alineado dentro de un contenedor fijo.
     */
    public static Node icon(String pathContent, String colorHex, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathContent);
        svg.setFill(Color.web(colorHex));

        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);

        StackPane container = new StackPane(svg);
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        container.setAlignment(Pos.CENTER);
        return container;
    }

    /**
     * Crea un badge de estado con bordes redondeados y estilo visual según la severidad/categoría.
     */
    public static Label badge(String text, String type) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("badge");
        switch (type.toLowerCase()) {
            case "open", "success" -> lbl.getStyleClass().add("badge-open");
            case "closed", "muted" -> lbl.getStyleClass().add("badge-closed");
            case "warning" -> lbl.getStyleClass().add("badge-warning");
            case "critical", "danger" -> lbl.getStyleClass().add("badge-critical");
            case "info", "normal" -> lbl.getStyleClass().add("badge-normal");
            case "purple" -> lbl.getStyleClass().add("badge-purple");
            default -> lbl.getStyleClass().add("badge-normal");
        }
        return lbl;
    }

    /**
     * Construye una tarjeta de métrica destacada (KPI Card) con subtítulo reactivo y dinámico.
     */
    public static VBox metricCard(String title, Label valueLabel, Label subtitleLabel, String colorHex, Node iconGraphic) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        card.setPrefWidth(240);
        card.setAlignment(Pos.TOP_LEFT);

        Region topStripe = new Region();
        topStripe.setPrefHeight(3);
        topStripe.setMaxHeight(3);
        topStripe.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 4 4 0 0;");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("metric-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (iconGraphic != null) {
            titleRow.getChildren().addAll(lblTitle, spacer, iconGraphic);
        } else {
            titleRow.getChildren().addAll(lblTitle, spacer);
        }

        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setStyle("-fx-text-fill: " + colorHex + ";");

        subtitleLabel.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 10px;");

        card.getChildren().addAll(topStripe, titleRow, valueLabel, subtitleLabel);
        return card;
    }

    /**
     * Sobrecarga estática con texto estático simple.
     */
    public static VBox metricCard(String title, Label valueLabel, String subtitle, String colorHex, Node iconGraphic) {
        return metricCard(title, valueLabel, new Label(subtitle), colorHex, iconGraphic);
    }
}
