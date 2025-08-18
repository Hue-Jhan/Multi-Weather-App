package com.example.meteoapp2.apis;

import com.example.meteoapp2.apis.CoordinatesHelper;
import com.example.meteoapp2.MainActivity;
import com.example.meteoapp2.datixd.CurrentWeatherData;
import com.example.meteoapp2.datixd.DailyWeatherData;
import com.example.meteoapp2.datixd.HourlyWeatherData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VisualCrossingAPI implements WeatherAPI {

    private String apiKey = "xxx";
    @Override
    public void getAllWeather(String city) throws Exception {
        String apiKey = "xxx";
        String baseUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";
        String url = baseUrl + city + "?unitGroup=us&include=days%2Ccurrent&key=" + apiKey + "&contentType=json";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());

            System.out.println("\nCity: " + json.getString("resolvedAddress"));
            System.out.println();

            JSONArray days = json.getJSONArray("days");
            for (int i = 0; i < days.length(); i++) {
                JSONObject day = days.getJSONObject(i);

                String date = day.getString("datetime");
                double tempMaxF = day.getDouble("tempmax");
                double tempMinF = day.getDouble("tempmin");
                double windSpeedMph = day.getDouble("windspeed");
                double windGustMph = day.optDouble("windgust", 0);
                double windDir = day.getDouble("winddir");
                String windDirCompass = windDirectionToCompass(windDir);
                String conditions = day.getString("conditions");
                String sunrise = day.getString("sunrise");
                String sunset = day.getString("sunset");
                double tempMaxC = Math.ceil(((tempMaxF - 32) * 5 / 9) * 10) / 10.0;
                double tempMinC = Math.ceil(((tempMinF - 32) * 5 / 9) * 10) / 10.0;
                double windSpeedKmh = windSpeedMph * 1.60934;
                double windGustKmh = windGustMph * 1.60934;

                System.out.printf(
                        "%s-> Max/Min T: %.0f/%.0f°C | Wind: %.1f/%.1f kmh %s | %s \n",
                        date, tempMaxC, tempMinC, windSpeedKmh, windGustKmh, windDirCompass, conditions);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return null;
        }
        double lat = coordinates[0];
        double lon = coordinates[1];
        String baseUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";
        String url = baseUrl + lat + "," + lon + "?unitGroup=metric&include=days%2Ccurrent&key=" + apiKey + "&contentType=json";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray days = json.getJSONArray("days");
            JSONObject current = json.getJSONObject("currentConditions");

            double temp = current.getDouble("temp");
            // double tempMinF = current.getDouble("tempmin");
            double windSpeedMph = current.getDouble("windspeed");
            // double windGustMph = current.optDouble("windgust", 0);
            int windDir = current.getInt("winddir");
            // String windDirCompass = windDirectionToCompass(windDir);
            String conditions = current.getString("conditions");
            double windSpeedKmh = windSpeedMph * 1.80934;
            windSpeedKmh = Math.round(windSpeedKmh * 10.0) / 10.0;
            // double windGustKmh = windGustMph * 1.60934;

            if (conditions.toLowerCase().contains("clear")) {
                conditions = "Sunny";
            }
            return new CurrentWeatherData(temp, windSpeedKmh, "VisualCrossing", conditions, windDir);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public double[] getCoordinates(String city) throws Exception {
        /*String url = "http://dataservice.accuweather.com/locations/v1/cities/IT/search"
                + "?apikey=" + "xxx"
                + "&q=" + city;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        JSONArray arr = new JSONArray(content.toString());
        if (arr.length() == 0) return null;

        JSONObject firstResult = arr.getJSONObject(0);
        if (arr.length() > 1 && city.equalsIgnoreCase("campomarino")) {
            firstResult = arr.getJSONObject(1);
        }

        String cityName = firstResult.getString("LocalizedName");
        String countryName = firstResult.getJSONObject("Country").getString("LocalizedName");
        String regionName = firstResult.getJSONObject("AdministrativeArea").getString("LocalizedName");
        double latitude = firstResult.getJSONObject("GeoPosition").getDouble("Latitude");
        double longitude = firstResult.getJSONObject("GeoPosition").getDouble("Longitude");

        // System.out.println("\nWeather for " + cityName + ", " + regionName + ", " + countryName + ":");
        return new double[]{latitude, longitude};*/
        return CoordinatesHelper.getCoordinates(city);
    }
    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return null;
        }
        double lat = coordinates[0];
        double lon = coordinates[1];
        String baseUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";
        String url = baseUrl + lat + "," + lon + "?unitGroup=metric&include=days%2Chours&key=" + apiKey + "&contentType=json";

        List<DailyWeatherData> result = new ArrayList<>();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray days = json.getJSONArray("days");
            String timezone = json.getString("timezone");

            for (int i = 0; i < days.length(); i++) {
                JSONObject day = days.getJSONObject(i);

                String date = day.getString("datetime");
                double tempMaxF = day.getDouble("tempmax");
                double tempMinF = day.getDouble("tempmin");
                double windSpeedMph = day.getDouble("windspeed");
                double windDir = day.getDouble("winddir");
                String conditions = day.getString("conditions");

                // double tempMaxC = Math.ceil(((tempMaxF - 32) * 5 / 9) * 10) / 10.0;
                // double tempMinC = Math.ceil(((tempMinF - 32) * 5 / 9) * 10) / 10.0;
                double windSpeedKmh = windSpeedMph * 1; // 1.60934 if US, but Visual Crossing metric should already be km/h
                windSpeedKmh = Math.round(windSpeedKmh * 10.0) / 10.0;

                if (conditions.toLowerCase().contains("clear")) {
                    conditions = "Sunny";
                }

                DailyWeatherData dwd = new DailyWeatherData(
                        date,
                        tempMaxF,
                        tempMinF,
                        windSpeedKmh,
                        (int) windDir,
                        conditions,
                        "VisualCrossing"
                );
                if (day.has("hours")) {
                    JSONArray hours = day.getJSONArray("hours");
                    List<HourlyWeatherData> hourlyList = new ArrayList<>();

                    for (int j = 0; j < hours.length(); j++) {
                        JSONObject hour = hours.getJSONObject(j);

                        String dayDate = day.getString("datetime"); // e.g. 2025-07-25
                        String timePart = hour.getString("datetime"); // e.g. 03:00:00

                        LocalDate datePart = LocalDate.parse(dayDate);
                        LocalTime time = LocalTime.parse(timePart);
                        LocalDateTime localDateTime = LocalDateTime.of(datePart, time);
                        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timezone));

                        String isoDatetime = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

                        double tempF = hour.getDouble("temp");
                        double windSpeedHourMph = hour.getDouble("windspeed");
                        double windDirHour = hour.getDouble("winddir");
                        String hourConditions = hour.getString("conditions");

                        // double tempC = Math.ceil(((tempF - 32) * 5 / 9) * 10) / 10.0;
                        double windSpeedHourKmh = windSpeedHourMph * 1;
                        windSpeedHourKmh = Math.round(windSpeedHourKmh * 10.0) / 10.0;

                        if (hourConditions.toLowerCase().contains("clear")) {
                            hourConditions = "Sunny";
                        }

                        HourlyWeatherData hwd = new HourlyWeatherData(
                                isoDatetime,  // Pass the ISO datetime string here
                                tempF,
                                windSpeedHourKmh,
                                (int) windDirHour,
                                hourConditions,
                                "VisualCrossing"
                        );
                        hourlyList.add(hwd);
                    }
                    dwd.hourlyData.addAll(hourlyList);
                }
                result.add(dwd);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    public static String windDirectionToCompass(double degrees) {
        String[] directions = {
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
        };
        int index = (int) Math.round(((degrees % 360) / 22.5));
        return directions[index % 16];
    }
}

