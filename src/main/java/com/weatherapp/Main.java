package com.weatherapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main JavaFX application for the Weather Information App.
 */
public class Main extends Application {
    private final WeatherService weatherService = new WeatherService();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f4f8ff, #eef4ff);");

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(700);
        content.setPrefWidth(700);
        content.setMinWidth(600);

        Label titleLabel = new Label("Weather Information App");
        titleLabel.getStyleClass().add("title");

        Label subtitleLabel = new Label("Retrieve real-time weather details for any city in seconds.");
        subtitleLabel.getStyleClass().add("subtitle");

        HBox searchBox = new HBox(12);
        searchBox.setAlignment(Pos.CENTER);

        TextField cityField = new TextField();
        cityField.setPromptText("Enter city name");
        cityField.setPrefWidth(360);
        cityField.setPrefHeight(40);

        Button searchButton = new Button("Search");
        searchButton.setPrefHeight(40);
        searchButton.getStyleClass().add("search-button");

        searchBox.getChildren().addAll(cityField, searchButton);

        VBox resultCard = new VBox(12);
        resultCard.getStyleClass().add("result-card");
        resultCard.setPadding(new Insets(24));
        resultCard.setSpacing(12);
        resultCard.setPrefWidth(620);

        Label cityCountryLabel = new Label("City and country will appear here");
        cityCountryLabel.getStyleClass().add("city-country");

        Label temperatureLabel = new Label("--°C");
        temperatureLabel.getStyleClass().add("temperature");

        Label conditionLabel = new Label("Condition: --");
        conditionLabel.getStyleClass().add("detail");

        Label descriptionLabel = new Label("Description: --");
        descriptionLabel.getStyleClass().add("detail");

        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10, 0, 0, 0));
        detailsBox.getChildren().addAll(
                new Label("Feels like: --°C"),
                new Label("Humidity: --%"),
                new Label("Wind speed: -- m/s"));

        Label statusLabel = new Label("Enter a city name and press Search to begin.");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setWrapText(true);

        Label footerLabel = new Label("Weather data provided by OpenWeatherMap");
        footerLabel.getStyleClass().add("footer");

        resultCard.getChildren().addAll(cityCountryLabel, temperatureLabel, conditionLabel, descriptionLabel, detailsBox);
        content.getChildren().addAll(titleLabel, subtitleLabel, searchBox, resultCard, statusLabel, footerLabel);
        root.setCenter(content);

        Scene scene = new Scene(root, 760, 760);
        URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setTitle("Weather Information App");
        stage.setScene(scene);
        stage.show();

        searchButton.setOnAction(event -> performSearch(cityField, resultCard, cityCountryLabel, statusLabel, searchButton));
        cityField.setOnAction(event -> performSearch(cityField, resultCard, cityCountryLabel, statusLabel, searchButton));
    }

    private void performSearch(TextField cityField, VBox resultCard, Label cityCountryLabel, Label statusLabel, Button searchButton) {
        String rawCity = cityField.getText();
        if (rawCity == null || rawCity.trim().isEmpty()) {
            showError(statusLabel, "Please enter a city name.");
            return;
        }

        String city = rawCity.trim();
        cityField.setText(city);

        setLoadingState(statusLabel, searchButton);

        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                return weatherService.getWeather(city);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            WeatherData weatherData = task.getValue();
            updateWeather(resultCard, cityCountryLabel, weatherData);
            statusLabel.setText("Weather loaded successfully for " + weatherData.getCity() + ".");
            statusLabel.getStyleClass().setAll("status-label", "status-success");
            searchButton.setDisable(false);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable exception = task.getException();
            String message;
            if (exception != null) {
                Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
                message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
            } else {
                message = "Unable to retrieve weather data.";
            }
            showError(statusLabel, message);
            searchButton.setDisable(false);
        }));

        Thread thread = new Thread(task, "weather-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void setLoadingState(Label statusLabel, Button searchButton) {
        statusLabel.setText("Loading weather data. Please wait...");
        statusLabel.getStyleClass().setAll("status-label", "status-loading");
        searchButton.setDisable(true);
    }

    private void showError(Label statusLabel, String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll("status-label", "status-error");
    }

    private void updateWeather(VBox resultCard, Label cityCountryLabel, WeatherData weatherData) {
        cityCountryLabel.setText(weatherData.getCity() + ", " + weatherData.getCountry());
        resultCard.getChildren().setAll(
                cityCountryLabel,
                createLabel("Temperature", String.format("%.1f°C", weatherData.getTemperature()), "temperature"),
                createLabel("Condition", weatherData.getCondition(), "detail"),
                createLabel("Description", weatherData.getDescription(), "detail"),
                createLabel("Feels like", String.format("%.1f°C", weatherData.getFeelsLike()), "detail"),
                createLabel("Humidity", weatherData.getHumidity() + "%", "detail"),
                createLabel("Wind speed", String.format("%.1f m/s", weatherData.getWindSpeed()), "detail")
        );
    }

    private Label createLabel(String title, String value, String styleClass) {
        Label label = new Label(title + ": " + value);
        label.getStyleClass().add(styleClass);
        return label;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
