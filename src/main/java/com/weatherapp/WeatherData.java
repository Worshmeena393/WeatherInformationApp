package com.weatherapp;

/**
 * Represents the weather information returned by the OpenWeatherMap API.
 */
public class WeatherData {
    private String city;
    private final String country;
    private final double temperature;
    private final double feelsLike;
    private final int humidity;
    private final double windSpeed;
    private final String condition;
    private final String description;

    public WeatherData(String city, String country, double temperature, double feelsLike,
                       int humidity, double windSpeed, String condition, String description) {
        this.city = city;
        this.country = country;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.description = description;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getCondition() {
        return condition;
    }

    public String getDescription() {
        return description;
    }
}
