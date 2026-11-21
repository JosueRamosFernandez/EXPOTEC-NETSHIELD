package com.netshield.ui;

/**
 * Clase lanzadora (Launcher) necesaria para que el Fat-JAR (shade)
 * funcione correctamente con JavaFX.
 * <p>
 * JavaFX requiere que la clase main del JAR ejecutable NO extienda
 * {@link javafx.application.Application} directamente cuando se empaqueta
 * sin module-info. Esta clase simple delega al verdadero punto de entrada.
 */
public class Launcher {
    public static void main(String[] args) {
        NetShieldApp.main(args);
    }
}
