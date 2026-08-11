# Weather Information App

## Overview
This project is a professional JavaFX desktop application that allows users to enter a city name and retrieve real-time weather information from the OpenWeatherMap Current Weather API.

## Features
- Enter a city name and search for live weather conditions
- Display city and country
- Show temperature, feels-like temperature, humidity, wind speed, weather condition, and description
- Provide friendly validation and error messages
- Use a polished JavaFX user interface with a separate stylesheet
- Run the app using Maven and JavaFX

## Technologies
- Java 23
- JavaFX
- Maven
- OpenWeatherMap API
- org.json for JSON parsing

## OpenWeatherMap API
The application uses the OpenWeatherMap Current Weather API endpoint:
https://api.openweathermap.org/data/2.5/weather

## API Key Configuration
Before running the application, configure an environment variable named OPENWEATHER_API_KEY.

Example (PowerShell):
```powershell
$env:OPENWEATHER_API_KEY="your_api_key_here"
```

Do not place the API key directly in the Java source code, README, or Maven configuration.

## How to Run
1. Install Java 23 and Maven.
2. Set the OPENWEATHER_API_KEY environment variable.
3. From the project root, run:
```bash
mvn clean javafx:run
```

## How to Use
1. Launch the application.
2. Enter a city name in the input field.
3. Click Search or press Enter.
4. Review the weather details displayed in the result card.

## Project Structure
WeatherInformationApp/
├── pom.xml
├── README.md
├── screenshots/
│   └── README.txt
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── weatherapp/
        │           ├── Main.java
        │           ├── WeatherService.java
        │           └── WeatherData.java
        └── resources/
            └── style.css

## Error Handling
The application handles:
- Empty input
- Missing API key
- Invalid API key
- Invalid or unknown city
- API and network failures
- Unexpected HTTP response errors

## Implementation Details
The application uses a JavaFX Task to perform the API request in the background so the JavaFX application thread remains responsive.

## Rubric Coverage
- API Integration: Uses the OpenWeatherMap Current Weather API and parses JSON data.
- GUI Design: Built with JavaFX and styled using a dedicated CSS file.
- Logic and Computation: Validates input, sends API requests, parses responses, and stores results in a WeatherData object.
- Program Flow and Structure: Uses separate classes for the main UI, API calls, and data model.
- Output: Provides complete source code, Maven configuration, and documentation.
- Code Style and Readability: Uses clear naming, comments, and organized structure.

## Author
Worshmeena Qayoumi

Course: CS 1103

Assignment: Unit 8 - Advanced GUI Programming
