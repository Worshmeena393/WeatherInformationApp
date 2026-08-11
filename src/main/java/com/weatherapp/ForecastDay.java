package com.weatherapp;

/**
 * Represents a single day's entry in the 5-day weather forecast.
 */
public class ForecastDay {
    private final String dayName;
    private final String dateLabel;
    private final String icon;
    private final String condition;
    private final double minTemp;
    private final double maxTemp;

    public ForecastDay(String dayName, String dateLabel, String icon, String condition,
                       double minTemp, double maxTemp) {
        this.dayName = dayName == null ? "" : dayName;
        this.dateLabel = dateLabel == null ? "" : dateLabel;
        this.icon = icon == null || icon.isBlank() ? "01d" : icon;
        this.condition = condition == null ? "Unknown" : condition;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public String getDayName() {
        return dayName;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getIcon() {
        return icon;
    }

    public String getCondition() {
        return condition;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }
}
