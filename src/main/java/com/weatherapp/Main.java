package com.weatherapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main JavaFX application for the Weather Information App — production-ready build:
 *  - Dynamic weather icons from OpenWeatherMap
 *  - Weather-driven backgrounds + Light/Dark mode toggle
 *  - 5-day forecast horizontal forecast cards (ScrollPane)
 *  - Auto-loads Kabul,AF as the default city on launch
 *  - Search history (history.json) rendered as a clickable ComboBox
 *  - Deployment-ready via shade plugin uber JAR (Launcher entry)
 */
public class Main extends Application {
    private static final String DEFAULT_CITY = "Kabul,AF";

    private final WeatherService weatherService = new WeatherService();
    private final SearchHistory searchHistory = new SearchHistory();
    private final Map<String, Image> iconCache = new HashMap<>();

    private BorderPane root;
    private Scene scene;
    private boolean darkMode = false;

    /* Primary UI nodes (kept as fields so we can update them) */
    private TextField cityField;
    private Button searchButton;
    private Button themeButton;
    private ComboBox<String> historyCombo;
    private Label statusLabel;

    private VBox resultCard;
    private Label cityCountryLabel;
    private Label temperatureLabel;
    private Label conditionLabel;
    private Label descriptionLabel;
    private Label feelsLikeLabel;
    private Label humidityLabel;
    private Label windLabel;
    private ImageView currentIconView;
    private HBox forecastBox;
    private Label dataSourceLabel;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setPadding(new Insets(16));
        root.getStyleClass().add("app-root");
        applyWeatherBackground("Clear", false);

        VBox content = new VBox(12);
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(760);
        content.setPrefWidth(760);
        content.setMinWidth(600);
        content.setPadding(new Insets(8, 10, 14, 10));

        Label titleLabel = new Label("Weather Information App");
        titleLabel.getStyleClass().add("title");

        Label subtitleLabel = new Label("Live forecasts, icons, themes, 5-day forecast, search history & more.");
        subtitleLabel.getStyleClass().add("subtitle");

        /* --- Top-bar: theme toggle on the right; title section on the left. */
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_RIGHT);
        themeButton = new Button("🌙  Dark Mode");
        themeButton.getStyleClass().add("theme-button");
        themeButton.setOnAction(e -> toggleTheme());
        headerRow.getChildren().add(themeButton);

        /* --- Search row: search field + search button + history combo. */
        cityField = new TextField();
        cityField.setPromptText("Enter city name (e.g. Kabul, London, Dubai)");
        cityField.setPrefHeight(38);
        cityField.getStyleClass().add("city-field");
        HBox.setHgrow(cityField, Priority.ALWAYS);

        searchButton = new Button("Search");
        searchButton.setPrefHeight(38);
        searchButton.getStyleClass().add("search-button");

        historyCombo = new ComboBox<>();
        historyCombo.setPrefHeight(38);
        historyCombo.setPrefWidth(190);
        historyCombo.setPromptText("⏱ Recent");
        historyCombo.getStyleClass().add("history-combo");
        historyCombo.setButtonCell(new HistoryListCell(true));
        historyCombo.setCellFactory(lv -> new HistoryListCell(false));
        historyCombo.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null && !nv.isEmpty()) {
                Platform.runLater(() -> {
                    historyCombo.getSelectionModel().clearSelection();
                    cityField.setText(nv);
                    performSearch(nv);
                });
            }
        });
        refreshHistoryCombo();

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getChildren().addAll(cityField, searchButton, historyCombo);

        /* --- Current weather result card with icon. */
        resultCard = new VBox(10);
        resultCard.getStyleClass().add("result-card");
        resultCard.setPadding(new Insets(16, 20, 16, 20));
        resultCard.setPrefWidth(720);

        cityCountryLabel = new Label("Loading your city…");
        cityCountryLabel.getStyleClass().add("city-country");

        currentIconView = new ImageView();
        currentIconView.setFitWidth(90);
        currentIconView.setFitHeight(90);
        currentIconView.setPreserveRatio(true);
        currentIconView.setSmooth(true);
        StackPane iconWrap = new StackPane(currentIconView);
        iconWrap.getStyleClass().add("icon-wrap");

        temperatureLabel = new Label("--°C");
        temperatureLabel.getStyleClass().add("temperature");

        HBox tempRow = new HBox(14);
        tempRow.setAlignment(Pos.CENTER_LEFT);
        tempRow.getChildren().addAll(iconWrap, temperatureLabel);

        conditionLabel = new Label("Condition: --");
        conditionLabel.getStyleClass().add("detail");
        descriptionLabel = new Label("Description: --");
        descriptionLabel.getStyleClass().add("detail");

        feelsLikeLabel = new Label("Feels like: --°C");
        feelsLikeLabel.getStyleClass().add("detail");
        humidityLabel = new Label("Humidity: --%");
        humidityLabel.getStyleClass().add("detail");
        windLabel = new Label("Wind speed: -- m/s");
        windLabel.getStyleClass().add("detail");

        HBox detailsRow = new HBox(18);
        detailsRow.getChildren().addAll(feelsLikeLabel, humidityLabel, windLabel);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        dataSourceLabel = new Label();
        dataSourceLabel.getStyleClass().add("data-source");

        resultCard.getChildren().addAll(
                cityCountryLabel, tempRow,
                conditionLabel, descriptionLabel,
                detailsRow, dataSourceLabel
        );

        /* --- 5-day forecast section. */
        Label forecastTitle = new Label("5-Day Forecast");
        forecastTitle.getStyleClass().add("forecast-title");

        forecastBox = new HBox(10);
        forecastBox.setAlignment(Pos.CENTER);
        forecastBox.setPadding(new Insets(2));
        for (int i = 0; i < 5; i++) {
            forecastBox.getChildren().add(buildPlaceholderForecastCard());
        }

        ScrollPane forecastScroll = new ScrollPane(forecastBox);
        forecastScroll.setFitToHeight(true);
        forecastScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        forecastScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        forecastScroll.getStyleClass().add("forecast-scroll");
        forecastScroll.setMaxHeight(175);
        forecastScroll.setPrefViewportHeight(165);

        VBox forecastSection = new VBox(6);
        forecastSection.getChildren().addAll(forecastTitle, forecastScroll);

        /* --- Status / footer. */
        statusLabel = new Label("Preparing default weather for " + DEFAULT_CITY + "…");
        statusLabel.getStyleClass().setAll("status-label", "status-loading");
        statusLabel.setWrapText(true);

        Label footerLabel = new Label("Weather data provided by OpenWeatherMap  •  Icons © OpenWeatherMap");
        footerLabel.getStyleClass().add("footer");

        content.getChildren().addAll(
                headerRow, titleLabel, subtitleLabel,
                searchBox, resultCard, forecastSection,
                statusLabel, footerLabel
        );
        VBox.setMargin(headerRow, new Insets(0, 0, -8, 0));
        VBox.setMargin(forecastSection, new Insets(2, 0, 0, 0));

        /* --- Scrollable page wrapper so content is never clipped. */
        ScrollPane pageScroll = new ScrollPane(content);
        pageScroll.setFitToWidth(true);
        pageScroll.setFitToHeight(false);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.getStyleClass().add("page-scroll");
        pageScroll.setPadding(new Insets(0));

        root.setCenter(pageScroll);

        scene = new Scene(root, 820, 680);
        scene.getStylesheets().addAll(loadStyleSheet(), loadLightDarkSheet());
        stage.setTitle("Weather Information App — Production Edition");
        stage.setScene(scene);
        stage.setMinWidth(680);
        stage.setMinHeight(600);
        stage.show();

        searchButton.setOnAction(e -> performSearch(cityField.getText()));
        cityField.setOnAction(e -> performSearch(cityField.getText()));

        Platform.runLater(() -> performSearch(DEFAULT_CITY));
    }

    /* ---------------- UI building helpers ---------------- */

    private VBox buildPlaceholderForecastCard() {
        VBox card = new VBox(4);
        card.getStyleClass().add("forecast-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 8, 10, 8));
        card.setPrefWidth(120);
        card.setPrefHeight(150);

        Label day = new Label("—");
        day.getStyleClass().add("forecast-day");
        Label date = new Label("");
        date.getStyleClass().add("forecast-date");

        ImageView iv = new ImageView();
        iv.setFitWidth(44);
        iv.setFitHeight(44);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        Label cond = new Label("--");
        cond.getStyleClass().add("forecast-condition");
        Label minMax = new Label("-- / --");
        minMax.getStyleClass().add("forecast-temps");

        card.getChildren().addAll(day, date, iv, cond, minMax);
        return card;
    }

    private void updateForecastCards(List<ForecastDay> forecast) {
        ObservableList<javafx.scene.Node> children = forecastBox.getChildren();
        children.clear();
        for (ForecastDay day : forecast) {
            VBox card = new VBox(4);
            card.getStyleClass().add("forecast-card");
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(10, 8, 10, 8));
            card.setPrefWidth(120);
            card.setPrefHeight(150);

            Label dayName = new Label(day.getDayName());
            dayName.getStyleClass().add("forecast-day");
            Label dateLabel = new Label(day.getDateLabel());
            dateLabel.getStyleClass().add("forecast-date");

            Image icon = loadIcon(day.getIcon());
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(44);
            iv.setFitHeight(44);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            Label cond = new Label(day.getCondition());
            cond.getStyleClass().add("forecast-condition");
            Label minMax = new Label(String.format(Locale.ROOT, "%.0f° / %.0f°",
                    day.getMinTemp(), day.getMaxTemp()));
            minMax.getStyleClass().add("forecast-temps");

            card.getChildren().addAll(dayName, dateLabel, iv, cond, minMax);
            children.add(card);
        }
    }

    /* ---------------- Search + async loading ---------------- */

    private void performSearch(String rawCity) {
        String raw = rawCity == null ? "" : rawCity;
        if (raw.trim().isEmpty()) {
            showError("Please enter a city name.");
            return;
        }
        String city = raw.trim();
        cityField.setText(city);
        setLoadingState();

        Task<CurrentAndForecast> task = new Task<>() {
            @Override
            protected CurrentAndForecast call() throws Exception {
                WeatherData current = weatherService.getWeather(city);
                List<ForecastDay> forecast = weatherService.getForecast(
                        current.getCity() + "," + current.getCountry());
                return new CurrentAndForecast(current, forecast);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            CurrentAndForecast cf = task.getValue();
            WeatherData w = cf.current;
            List<ForecastDay> f = cf.forecast;
            updateCurrentWeather(w);
            updateForecastCards(f);
            applyWeatherBackground(w.getCondition(), w.isNightIcon());
            searchHistory.add(w.getCity(), w.getCountry());
            refreshHistoryCombo();
            statusLabel.setText("Loaded weather for " + w.getCity() + ", " + w.getCountry() + ".");
            statusLabel.getStyleClass().setAll("status-label", "status-success");
            searchButton.setDisable(false);
            dataSourceLabel.setText(weatherService.lastUsedMock()
                    ? "Data source: Demo / Mock data (set a valid OpenWeatherMap key for live data)"
                    : "Data source: OpenWeatherMap live API");
            dataSourceLabel.getStyleClass().setAll("data-source",
                    weatherService.lastUsedMock() ? "data-source-demo" : "data-source-live");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable t = task.getException();
            Throwable cause = (t != null && t.getCause() != null) ? t.getCause() : t;
            String message = (cause != null && cause.getMessage() != null)
                    ? cause.getMessage() : "Unable to retrieve weather data.";
            showError(message);
            searchButton.setDisable(false);
        }));

        Thread thread = new Thread(task, "weather-search-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateCurrentWeather(WeatherData w) {
        cityCountryLabel.setText(w.getCity() + ", " + w.getCountry());
        temperatureLabel.setText(String.format(Locale.ROOT, "%.1f°C", w.getTemperature()));
        conditionLabel.setText("Condition: " + w.getCondition());
        descriptionLabel.setText("Description: " + capitalizeFirst(w.getDescription()));
        feelsLikeLabel.setText("Feels like: " + String.format(Locale.ROOT, "%.1f°C", w.getFeelsLike()));
        humidityLabel.setText("Humidity: " + w.getHumidity() + "%");
        windLabel.setText("Wind speed: " + String.format(Locale.ROOT, "%.1f m/s", w.getWindSpeed()));
        currentIconView.setImage(loadIcon(w.getIcon()));
    }

    /* ---------------- Theming + background ---------------- */

    private void applyWeatherBackground(String condition, boolean night) {
        String base = switch (condition == null ? "" : condition) {
            case "Rain", "Drizzle", "Thunderstorm" -> night ? "bg-rain-night" : "bg-rain";
            case "Snow" -> night ? "bg-snow-night" : "bg-snow";
            case "Clouds" -> night ? "bg-clouds-night" : "bg-clouds";
            case "Clear" -> night ? "bg-clear-night" : "bg-clear";
            case "Mist", "Fog", "Haze", "Smoke" -> night ? "bg-fog-night" : "bg-fog";
            default -> night ? "bg-default-night" : "bg-default";
        };
        ObservableList<String> styleClasses = root.getStyleClass();
        styleClasses.removeIf(s -> s.startsWith("bg-"));
        styleClasses.add(base);
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        ObservableList<String> styles = scene.getStylesheets();
        styles.removeIf(s -> s.contains("style-dark"));
        if (darkMode) {
            styles.add(loadDarkSheet());
            themeButton.setText("☀️  Light Mode");
            root.getStyleClass().remove("theme-light");
            root.getStyleClass().add("theme-dark");
        } else {
            themeButton.setText("🌙  Dark Mode");
            root.getStyleClass().remove("theme-dark");
            root.getStyleClass().add("theme-light");
        }
    }

    /* ---------------- History dropdown ---------------- */

    private void refreshHistoryCombo() {
        List<String> labels = searchHistory.getCityLabels();
        Platform.runLater(() -> {
            String prompt = labels.isEmpty() ? "⏱  Recent" : "⏱  Recent searches";
            historyCombo.setItems(FXCollections.observableArrayList(labels));
            if (!historyCombo.isShowing()) {
                // keep current selection
            }
            historyCombo.setPromptText(prompt);
        });
    }

    private static final class HistoryListCell extends ListCell<String> {
        private final boolean buttonCell;

        HistoryListCell(boolean buttonCell) {
            this.buttonCell = buttonCell;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null || item.isBlank()) {
                setText(null);
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                if (buttonCell) {
                    setStyle(null);
                }
                return;
            }
            setText(item);
            setContentDisplay(ContentDisplay.LEFT);
            getStyleClass().add("history-cell");
        }
    }

    /* ---------------- Helpers ---------------- */

    private Image loadIcon(String iconCode) {
        String code = (iconCode == null || iconCode.isBlank()) ? "01d" : iconCode;
        return iconCache.computeIfAbsent(code, k -> {
            String url = WeatherService.iconUrl(k);
            try {
                return new Image(url, 128, 128, true, true, true);
            } catch (Exception ex) {
                System.err.println("Failed to load icon " + code + ": " + ex.getMessage());
                return new Image("https://openweathermap.org/img/wn/01d@2x.png", 128, 128, true, true, true);
            }
        });
    }

    private String loadStyleSheet() {
        URL css = getClass().getResource("/style.css");
        return css == null ? "" : css.toExternalForm();
    }

    private String loadLightDarkSheet() {
        // No separate stylesheet; dark mode is driven by style-classes on root
        return "";
    }

    private String loadDarkSheet() {
        return "";
    }

    private void setLoadingState() {
        statusLabel.setText("Loading weather data. Please wait…");
        statusLabel.getStyleClass().setAll("status-label", "status-loading");
        searchButton.setDisable(true);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll("status-label", "status-error");
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() == 1) return s.toUpperCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static final class CurrentAndForecast {
        final WeatherData current;
        final List<ForecastDay> forecast;

        CurrentAndForecast(WeatherData current, List<ForecastDay> forecast) {
            this.current = current;
            this.forecast = forecast;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
