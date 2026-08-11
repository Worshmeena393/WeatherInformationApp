package com.weatherapp;

/**
 * Entry point for the shaded executable JAR.
 *
 * JavaFX applications shipped as a shaded/uber JAR must NOT subclass Application
 * in the Main-Class listed in the JAR manifest, otherwise the launcher fails with
 * "Error: JavaFX runtime components are missing". Launcher is a plain class that
 * explicitly starts the JavaFX runtime via Application.launch(Main.class, args).
 */
public class Launcher {
    public static void main(String[] args) {
        Main.launch(Main.class, args);
    }
}
