package com.weatherapp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles communication with the OpenWeatherMap Current Weather + 5-Day Forecast APIs.
 * Falls back to realistic mock data when no valid API key is available.
 */
public class WeatherService {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";
    public static final String OPENWEATHER_ICON_URL = "https://openweathermap.org/img/wn/%s@2x.png";
    private static final List<String> COUNTRY_FALLBACKS = Arrays.asList(
            "AF", "PK", "IN", "IR", "US", "GB", "DE", "FR", "CA", "AU", "AE", "SA"
    );
    private static final List<String> CONDITIONS = Arrays.asList(
            "Clear", "Clouds", "Rain", "Drizzle", "Thunderstorm",
            "Snow", "Mist", "Smoke", "Haze", "Fog"
    );
    private static final Map<String, String> CONDITION_DESCRIPTIONS = new HashMap<>();
    static {
        CONDITION_DESCRIPTIONS.put("Clear", "clear sky");
        CONDITION_DESCRIPTIONS.put("Clouds", "scattered clouds");
        CONDITION_DESCRIPTIONS.put("Rain", "light rain");
        CONDITION_DESCRIPTIONS.put("Drizzle", "shower drizzle");
        CONDITION_DESCRIPTIONS.put("Thunderstorm", "thunderstorm with rain");
        CONDITION_DESCRIPTIONS.put("Snow", "light snow");
        CONDITION_DESCRIPTIONS.put("Mist", "mist");
        CONDITION_DESCRIPTIONS.put("Smoke", "smoke");
        CONDITION_DESCRIPTIONS.put("Haze", "haze");
        CONDITION_DESCRIPTIONS.put("Fog", "fog");
    }
    private static final Map<String, String> CONDITION_ICON_DAY = new HashMap<>();
    private static final Map<String, String> CONDITION_ICON_NIGHT = new HashMap<>();
    static {
        CONDITION_ICON_DAY.put("Clear", "01d");
        CONDITION_ICON_NIGHT.put("Clear", "01n");
        CONDITION_ICON_DAY.put("Clouds", "02d");
        CONDITION_ICON_NIGHT.put("Clouds", "02n");
        CONDITION_ICON_DAY.put("Rain", "10d");
        CONDITION_ICON_NIGHT.put("Rain", "10n");
        CONDITION_ICON_DAY.put("Drizzle", "09d");
        CONDITION_ICON_NIGHT.put("Drizzle", "09n");
        CONDITION_ICON_DAY.put("Thunderstorm", "11d");
        CONDITION_ICON_NIGHT.put("Thunderstorm", "11n");
        CONDITION_ICON_DAY.put("Snow", "13d");
        CONDITION_ICON_NIGHT.put("Snow", "13n");
        CONDITION_ICON_DAY.put("Mist", "50d");
        CONDITION_ICON_NIGHT.put("Mist", "50n");
        CONDITION_ICON_DAY.put("Smoke", "50d");
        CONDITION_ICON_NIGHT.put("Smoke", "50n");
        CONDITION_ICON_DAY.put("Haze", "50d");
        CONDITION_ICON_NIGHT.put("Haze", "50n");
        CONDITION_ICON_DAY.put("Fog", "50d");
        CONDITION_ICON_NIGHT.put("Fog", "50n");
    }
    private static final Map<String, String> CITY_COUNTRY_OVERRIDES = new HashMap<>();
    static {
        CITY_COUNTRY_OVERRIDES.put("kabul", "AF");
        CITY_COUNTRY_OVERRIDES.put("herat", "AF");
        CITY_COUNTRY_OVERRIDES.put("kandahar", "AF");
        CITY_COUNTRY_OVERRIDES.put("mazar-i-sharif", "AF");
        CITY_COUNTRY_OVERRIDES.put("jalalabad", "AF");
        CITY_COUNTRY_OVERRIDES.put("london", "GB");
        CITY_COUNTRY_OVERRIDES.put("paris", "FR");
        CITY_COUNTRY_OVERRIDES.put("berlin", "DE");
        CITY_COUNTRY_OVERRIDES.put("new york", "US");
        CITY_COUNTRY_OVERRIDES.put("newyork", "US");
        CITY_COUNTRY_OVERRIDES.put("los angeles", "US");
        CITY_COUNTRY_OVERRIDES.put("losangeles", "US");
        CITY_COUNTRY_OVERRIDES.put("chicago", "US");
        CITY_COUNTRY_OVERRIDES.put("toronto", "CA");
        CITY_COUNTRY_OVERRIDES.put("sydney", "AU");
        CITY_COUNTRY_OVERRIDES.put("melbourne", "AU");
        CITY_COUNTRY_OVERRIDES.put("dubai", "AE");
        CITY_COUNTRY_OVERRIDES.put("abu dhabi", "AE");
        CITY_COUNTRY_OVERRIDES.put("abudhabi", "AE");
        CITY_COUNTRY_OVERRIDES.put("riyadh", "SA");
        CITY_COUNTRY_OVERRIDES.put("jeddah", "SA");
        CITY_COUNTRY_OVERRIDES.put("islamabad", "PK");
        CITY_COUNTRY_OVERRIDES.put("karachi", "PK");
        CITY_COUNTRY_OVERRIDES.put("lahore", "PK");
        CITY_COUNTRY_OVERRIDES.put("mumbai", "IN");
        CITY_COUNTRY_OVERRIDES.put("delhi", "IN");
        CITY_COUNTRY_OVERRIDES.put("new delhi", "IN");
        CITY_COUNTRY_OVERRIDES.put("bangalore", "IN");
        CITY_COUNTRY_OVERRIDES.put("tehran", "IR");
    }
    private static final Map<String, Double> CITY_TEMPERATURE_BASE = new HashMap<>();
    static {
        CITY_TEMPERATURE_BASE.put("kabul", 24.0);
        CITY_TEMPERATURE_BASE.put("herat", 28.0);
        CITY_TEMPERATURE_BASE.put("kandahar", 32.0);
        CITY_TEMPERATURE_BASE.put("mazar-i-sharif", 29.0);
        CITY_TEMPERATURE_BASE.put("jalalabad", 33.0);
        CITY_TEMPERATURE_BASE.put("london", 15.0);
        CITY_TEMPERATURE_BASE.put("paris", 17.0);
        CITY_TEMPERATURE_BASE.put("berlin", 14.0);
        CITY_TEMPERATURE_BASE.put("new york", 18.0);
        CITY_TEMPERATURE_BASE.put("newyork", 18.0);
        CITY_TEMPERATURE_BASE.put("los angeles", 22.0);
        CITY_TEMPERATURE_BASE.put("losangeles", 22.0);
        CITY_TEMPERATURE_BASE.put("chicago", 16.0);
        CITY_TEMPERATURE_BASE.put("toronto", 13.0);
        CITY_TEMPERATURE_BASE.put("sydney", 19.0);
        CITY_TEMPERATURE_BASE.put("melbourne", 17.0);
        CITY_TEMPERATURE_BASE.put("dubai", 35.0);
        CITY_TEMPERATURE_BASE.put("abu dhabi", 36.0);
        CITY_TEMPERATURE_BASE.put("abudhabi", 36.0);
        CITY_TEMPERATURE_BASE.put("riyadh", 38.0);
        CITY_TEMPERATURE_BASE.put("jeddah", 34.0);
        CITY_TEMPERATURE_BASE.put("islamabad", 27.0);
        CITY_TEMPERATURE_BASE.put("karachi", 30.0);
        CITY_TEMPERATURE_BASE.put("lahore", 29.0);
        CITY_TEMPERATURE_BASE.put("mumbai", 28.0);
        CITY_TEMPERATURE_BASE.put("delhi", 30.0);
        CITY_TEMPERATURE_BASE.put("new delhi", 30.0);
        CITY_TEMPERATURE_BASE.put("bangalore", 25.0);
        CITY_TEMPERATURE_BASE.put("tehran", 26.0);
    }

    private final HttpClient httpClient;
    private volatile boolean lastUsedMock = false;

    public WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean lastUsedMock() {
        return lastUsedMock;
    }

    public static String iconUrl(String iconCode) {
        return String.format(Locale.ROOT, OPENWEATHER_ICON_URL, iconCode);
    }

    public WeatherData getWeather(String cityName) throws Exception {
        String trimmedCity = cityName == null ? "" : cityName.trim();
        if (trimmedCity.isEmpty()) {
            throw new IllegalArgumentException("Please enter a city name.");
        }

        String apiKey = loadApiKey();
        boolean hasValidKey = isValidApiKey(apiKey);

        if (hasValidKey) {
            apiKey = apiKey.trim();
            logKeyLoaded(apiKey);

            WeatherData result = tryFetchWeather(trimmedCity, apiKey);
            if (result != null) {
                lastUsedMock = false;
                return result;
            }

            if (!trimmedCity.contains(",")) {
                System.out.println("Direct lookup failed. Trying country code fallbacks...");
                for (String countryCode : COUNTRY_FALLBACKS) {
                    String qualified = trimmedCity + "," + countryCode;
                    result = tryFetchWeather(qualified, apiKey);
                    if (result != null) {
                        System.out.println("Successfully resolved via country code: " + qualified);
                        lastUsedMock = false;
                        return result;
                    }
                }
            }
        } else {
            System.out.println("No valid API key detected. Using mock weather data for demo.");
        }

        lastUsedMock = true;
        System.out.println("Falling back to mock/demo current weather for: " + trimmedCity);
        return generateMockWeather(trimmedCity);
    }

    public List<ForecastDay> getForecast(String cityName) throws Exception {
        String trimmedCity = cityName == null ? "" : cityName.trim();
        if (trimmedCity.isEmpty()) {
            throw new IllegalArgumentException("Please enter a city name.");
        }

        String apiKey = loadApiKey();
        boolean hasValidKey = isValidApiKey(apiKey);

        if (hasValidKey) {
            apiKey = apiKey.trim();

            List<ForecastDay> result = tryFetchForecast(trimmedCity, apiKey);
            if (result != null && !result.isEmpty()) {
                return result;
            }

            if (!trimmedCity.contains(",")) {
                for (String countryCode : COUNTRY_FALLBACKS) {
                    String qualified = trimmedCity + "," + countryCode;
                    result = tryFetchForecast(qualified, apiKey);
                    if (result != null && !result.isEmpty()) {
                        System.out.println("Forecast resolved via country code: " + qualified);
                        return result;
                    }
                }
            }
        }

        System.out.println("Falling back to mock/demo 5-day forecast for: " + trimmedCity);
        return generateMockForecast(trimmedCity);
    }

    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return false;
        String k = apiKey.trim().toLowerCase(Locale.ROOT);
        return !k.equalsIgnoreCase("YOUR_KEY")
                && !k.contains("your")
                && !k.contains("_key")
                && !k.contains("placeholder")
                && !k.contains("example")
                && !k.contains("demo")
                && !k.contains("test")
                && !k.contains("sample")
                && !k.contains("real_");
    }

    private void logKeyLoaded(String apiKey) {
        String masked = maskApiKey(apiKey);
        System.out.println("Key loaded: " + masked + " (Length: " + apiKey.length() + "). Attempting live API...");
    }

    private WeatherData tryFetchWeather(String query, String apiKey) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format(Locale.ROOT, "%s?q=%s&appid=%s&units=metric", BASE_URL, encoded, apiKey);
        logRequest(url, apiKey, query);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();
            System.out.println("Response status for '" + query + "': " + status);
            if (status != 200) {
                String msg = safeErrorMessage(body, status);
                System.out.println("Lookup failed for '" + query + "': " + msg);
                return null;
            }
            return parseWeatherResponse(body);
        } catch (IOException e) {
            System.out.println("Network error for '" + query + "': " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Request interrupted for '" + query + "'");
            return null;
        } catch (Exception e) {
            System.out.println("Unexpected error for '" + query + "': " + e.getMessage());
            return null;
        }
    }

    private List<ForecastDay> tryFetchForecast(String query, String apiKey) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format(Locale.ROOT, "%s?q=%s&appid=%s&units=metric", FORECAST_URL, encoded, apiKey);
        logRequest(url, apiKey, "forecast:" + query);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();
            System.out.println("Forecast status for '" + query + "': " + status);
            if (status != 200) {
                String msg = safeErrorMessage(body, status);
                System.out.println("Forecast failed for '" + query + "': " + msg);
                return null;
            }
            return parseForecastResponse(body);
        } catch (IOException e) {
            System.out.println("Network error for forecast '" + query + "': " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Forecast interrupted for '" + query + "'");
            return null;
        } catch (Exception e) {
            System.out.println("Unexpected forecast error for '" + query + "': " + e.getMessage());
            return null;
        }
    }

    private void logRequest(String url, String apiKey, String tag) {
        String safe = url.replace(apiKey, maskApiKey(apiKey));
        System.out.println("Request [" + tag + "]: " + safe);
    }

    private String safeErrorMessage(String body, int status) {
        try {
            JSONObject err = new JSONObject(body);
            return err.optString("message", "HTTP " + status);
        } catch (Exception e) {
            return "HTTP " + status;
        }
    }

    private WeatherData parseWeatherResponse(String body) {
        JSONObject root = new JSONObject(body);
        JSONArray weatherArray = root.optJSONArray("weather");
        JSONObject weatherDetails = (weatherArray != null && weatherArray.length() > 0)
                ? weatherArray.optJSONObject(0) : new JSONObject();
        JSONObject main = safeObj(root.optJSONObject("main"));
        JSONObject wind = safeObj(root.optJSONObject("wind"));
        JSONObject sys = root.optJSONObject("sys");

        String city = root.optString("name", "Unknown");
        String country = sys != null ? sys.optString("country", "Unknown") : "Unknown";
        String condition = weatherDetails.optString("main", "Unknown");
        String description = weatherDetails.optString("description", "No description available");
        String icon = weatherDetails.optString("icon", "01d");

        return new WeatherData(
                city, country,
                main.optDouble("temp", 0.0),
                main.optDouble("feels_like", 0.0),
                main.optInt("humidity", 0),
                wind.optDouble("speed", 0.0),
                condition, description, icon
        );
    }

    private List<ForecastDay> parseForecastResponse(String body) {
        JSONObject root = new JSONObject(body);
        JSONArray list = root.optJSONArray("list");
        if (list == null || list.isEmpty()) return new ArrayList<>();

        Map<LocalDate, DayAgg> buckets = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.optJSONObject(i);
            if (entry == null) continue;

            String dtTxt = entry.optString("dt_txt", "");
            LocalDate date;
            try {
                date = LocalDate.parse(dtTxt.substring(0, 10), fmt);
            } catch (Exception ex) {
                long dt = entry.optLong("dt", 0L);
                date = dt > 0
                        ? Instant.ofEpochSecond(dt).atZone(ZoneId.systemDefault()).toLocalDate()
                        : LocalDate.now().plusDays(i / 8);
            }

            JSONObject main = safeObj(entry.optJSONObject("main"));
            JSONArray weatherArr = entry.optJSONArray("weather");
            JSONObject weather = (weatherArr != null && weatherArr.length() > 0)
                    ? weatherArr.optJSONObject(0) : new JSONObject();

            double tempMin = main.optDouble("temp_min", Double.POSITIVE_INFINITY);
            double tempMax = main.optDouble("temp_max", Double.NEGATIVE_INFINITY);
            String cond = weather.optString("main", "Unknown");
            String icon = weather.optString("icon", "01d");
            int hour = dtTxt.length() >= 13 ? Integer.parseInt(dtTxt.substring(11, 13)) : 12;

            DayAgg agg = buckets.computeIfAbsent(date, k -> new DayAgg());
            agg.min = Math.min(agg.min, tempMin);
            agg.max = Math.max(agg.max, tempMax);
            int noonDist = Math.abs(hour - 12);
            if (noonDist < agg.noonDistance || agg.condition == null) {
                agg.noonDistance = noonDist;
                agg.condition = cond;
                agg.icon = icon;
            }
        }

        List<ForecastDay> out = new ArrayList<>();
        List<Map.Entry<LocalDate, DayAgg>> sorted = new ArrayList<>(buckets.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<LocalDate, DayAgg> e : sorted) {
            LocalDate d = e.getKey();
            DayAgg a = e.getValue();
            double min = Double.isInfinite(a.min) ? 0.0 : Math.round(a.min * 10.0) / 10.0;
            double max = Double.isInfinite(a.max) ? 0.0 : Math.round(a.max * 10.0) / 10.0;
            String dayName = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            String dateLabel = d.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
            out.add(new ForecastDay(dayName, dateLabel, a.icon, a.condition, min, max));
        }
        if (out.size() > 5) out = new ArrayList<>(out.subList(0, 5));
        return out;
    }

    private static final class DayAgg {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        String condition;
        String icon = "01d";
        int noonDistance = 99;
    }

    private JSONObject safeObj(JSONObject obj) {
        return obj == null ? new JSONObject() : obj;
    }

    /* ---------- Mock data generators ---------- */

    private WeatherData generateMockWeather(String query) {
        String cityPart = query;
        String countryPart = "XX";
        if (query.contains(",")) {
            String[] parts = query.split(",", 2);
            cityPart = parts[0].trim();
            countryPart = parts[1].trim().toUpperCase(Locale.ROOT);
        }

        String cityLookup = cityPart.toLowerCase(Locale.ROOT);
        String countryOverride = CITY_COUNTRY_OVERRIDES.get(cityLookup);
        if (countryOverride != null && "XX".equals(countryPart)) {
            countryPart = countryOverride;
        }

        String displayCity = capitalizeWords(cityPart);
        int seed = (displayCity + "|" + LocalDate.now().toString() + "|" + countryPart).hashCode();
        Random rnd = new Random(seed);

        Double baseTemp = CITY_TEMPERATURE_BASE.get(cityLookup);
        if (baseTemp == null) baseTemp = 15.0 + (rnd.nextInt(200) / 10.0);

        double temperature = round1(baseTemp + (rnd.nextInt(70) - 35) / 10.0);
        double feelsLike = round1(temperature + (rnd.nextInt(60) - 30) / 10.0);
        int humidity = 30 + rnd.nextInt(61);
        double windSpeed = round1(0.5 + rnd.nextInt(80) / 10.0);

        String condition = pickCondition(displayCity, countryPart, temperature, humidity, rnd);
        String description = CONDITION_DESCRIPTIONS.getOrDefault(condition, "overcast clouds");

        int hourNow = java.time.LocalTime.now().getHour();
        boolean night = hourNow < 6 || hourNow >= 20;
        String icon = iconForCondition(condition, night);

        WeatherData mock = new WeatherData(displayCity, countryPart, temperature, feelsLike,
                humidity, windSpeed, condition, description, icon);

        System.out.println("Mock current: " + displayCity + "," + countryPart
                + " → " + temperature + "°C, " + condition + " (" + description + "), icon " + icon
                + ", humidity " + humidity + "%, wind " + windSpeed + " m/s");
        return mock;
    }

    private List<ForecastDay> generateMockForecast(String query) {
        String cityLookup = (query.contains(",") ? query.split(",", 2)[0] : query).trim()
                .toLowerCase(Locale.ROOT);
        Double baseTemp = CITY_TEMPERATURE_BASE.get(cityLookup);
        int seed = (cityLookup + "|forecast|" + LocalDate.now()).hashCode();
        Random rnd = new Random(seed);

        List<ForecastDay> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            LocalDate d = today.plusDays(i);
            double base = baseTemp == null ? 17.0 : baseTemp;
            double wobble = (rnd.nextInt(70) - 35) / 10.0;
            double dayMax = round1(base + wobble + (rnd.nextInt(20) / 10.0));
            double dayMin = round1(dayMax - (3 + rnd.nextInt(70) / 10.0));
            boolean night = false;
            String condition = pickCondition("", "", (dayMin + dayMax) / 2, 40 + rnd.nextInt(40), rnd);
            String icon = iconForCondition(condition, night);
            DayOfWeek dow = d.getDayOfWeek();
            String dayName = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            String dateLabel = d.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
            out.add(new ForecastDay(dayName, dateLabel, icon, condition, dayMin, dayMax));
        }
        System.out.println("Mock forecast generated (" + out.size() + " days) for " + query);
        return out;
    }

    private String pickCondition(String displayCity, String countryPart, double temperature,
                                 int humidity, Random rnd) {
        if ("AF".equals(countryPart) && "Kabul".equalsIgnoreCase(displayCity)) {
            double p = rnd.nextDouble();
            return p < 0.40 ? "Clear"
                    : p < 0.70 ? "Haze"
                    : p < 0.85 ? "Smoke" : "Clouds";
        }
        if (temperature <= 0) return rnd.nextBoolean() ? "Snow" : "Clouds";
        if (humidity > 80 && rnd.nextBoolean()) return rnd.nextDouble() < 0.6 ? "Rain" : "Thunderstorm";
        if (humidity > 65) return CONDITIONS.get(rnd.nextInt(CONDITIONS.size()));
        double p = rnd.nextDouble();
        return p < 0.55 ? "Clear" : p < 0.75 ? "Clouds" : CONDITIONS.get(rnd.nextInt(CONDITIONS.size()));
    }

    private String iconForCondition(String condition, boolean night) {
        Map<String, String> table = night ? CONDITION_ICON_NIGHT : CONDITION_ICON_DAY;
        String icon = table.get(condition);
        return icon == null ? (night ? "03n" : "03d") : icon;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '\'') {
                sb.append(c);
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private String loadApiKey() {
        Path envFile = Paths.get(".env");
        String envFileKey = null;
        if (Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
                    String[] parts = trimmed.split("=", 2);
                    if (parts.length == 2 && parts[0].trim().equals("OPENWEATHER_API_KEY")) {
                        envFileKey = parts[1].trim();
                        break;
                    }
                }
            } catch (IOException e) { /* ignore */ }
        }
        String envVarKey = System.getenv("OPENWEATHER_API_KEY");
        if (envVarKey != null && !envVarKey.isBlank() && !"YOUR_KEY".equalsIgnoreCase(envVarKey.trim())) {
            return envVarKey;
        }
        if (envFileKey != null && !envFileKey.isBlank() && !"YOUR_KEY".equalsIgnoreCase(envFileKey.trim())) {
            return envFileKey;
        }
        return (envVarKey != null && !envVarKey.isBlank()) ? envVarKey : envFileKey;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) return apiKey == null ? "" : apiKey;
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
