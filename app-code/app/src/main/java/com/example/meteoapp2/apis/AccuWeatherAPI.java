package com.example.meteoapp2.apis;

import com.example.meteoapp2.datixd.CurrentWeatherData;
import com.example.meteoapp2.datixd.DailyWeatherData;
import com.example.meteoapp2.datixd.HourlyWeatherData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AccuWeatherAPI implements WeatherAPI {

    // private static final String API_KEY = "xxx";
    private static final String API_KEY = "xxx";
    private String source = "AccuWeatherAPI";
    public String regionData = "";

    @Override
    public void getAllWeather(String city) throws Exception {
        String locationKey = getLocationKey(city);
        if (locationKey == null) {
            System.out.println("City not found.");
            return;
        }

        getCurrentConditions(locationKey);
        getDailyForecastSium(locationKey);
        getFiveDayForecast(locationKey);
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        String locationKey = getLocationKey(city);
        if (locationKey == null) {
            System.out.println("City not found.");
            return null;
        }
        return getCurrentConditions2(locationKey);
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        String locationKey = getLocationKey(city);
        if (locationKey == null) {
            System.out.println("City not found.");
            return null;
        }
        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/" + locationKey
                + "?apikey=" + API_KEY + "&language=en&details=true&metric=true";

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

        JSONObject forecast = new JSONObject(content.toString());
        JSONArray dailyForecasts = forecast.getJSONArray("DailyForecasts");

        List<DailyWeatherData> result = new ArrayList<>();
        for (int i = 0; i < dailyForecasts.length(); i++) {
            JSONObject day = dailyForecasts.getJSONObject(i);
            String date = day.getString("Date").split("T")[0];

            JSONObject temp = day.getJSONObject("Temperature");
            double min = temp.getJSONObject("Minimum").getDouble("Value");
            double max = temp.getJSONObject("Maximum").getDouble("Value");

            String dayPhrase = day.getJSONObject("Day").getString("IconPhrase");
            // String nightPhrase = day.getJSONObject("Night").getString("IconPhrase");

            double dayWind = day.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
            // double nightWind = day.getJSONObject("Night").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
            int windDir = day.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Direction").getInt("Degrees");

            //System.out.println(date + ": Max/Min: " + max + "/" + min + "°C | Wind: " + dayWind + "km/h " + windDir + " | Weather: " + dayPhrase);
            DailyWeatherData dwd = new DailyWeatherData(
                    date, max, min, dayWind, windDir, dayPhrase, "AccuWeather");
            result.add(dwd);
        }
        if (!result.isEmpty()) {
            DailyWeatherData firstDay = result.get(0);
            List<HourlyWeatherData> hourlyData = getHourlyData(locationKey);
            if (hourlyData != null) {
                firstDay.hourlyData.addAll(hourlyData);
            }
        }
        return result;
    }

    private List<HourlyWeatherData> getHourlyData(String locationKey) throws Exception {
        String hourlyUrl = "http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/"
                + locationKey + "?apikey=" + API_KEY + "&details=true&metric=true";

        HttpURLConnection conn = (HttpURLConnection) new URL(hourlyUrl).openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        JSONArray hourlyArray = new JSONArray(content.toString());
        List<HourlyWeatherData> hourlyList = new ArrayList<>();

        for (int i = 0; i < hourlyArray.length(); i++) {
            JSONObject h = hourlyArray.getJSONObject(i);

            String dateTime = h.getString("DateTime");
            double temp = h.getJSONObject("Temperature").getDouble("Value");
            double windSpeed = h.getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
            int windDir = h.getJSONObject("Wind").getJSONObject("Direction").getInt("Degrees");
            String phrase = h.getString("IconPhrase");

            HourlyWeatherData hour = new HourlyWeatherData(
                    dateTime, temp, windSpeed, windDir, phrase, "AccuWeather");
            hourlyList.add(hour);
        }
        return hourlyList;
    }

    private String getLocationKey(String city) throws Exception {
        String url = "http://dataservice.accuweather.com/locations/v1/cities/IT/search"
                + "?apikey=" + API_KEY
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

        String locationKey = firstResult.getString("Key");
        String cityName = firstResult.getString("LocalizedName");
        String countryName = firstResult.getJSONObject("Country").getString("LocalizedName");
        String regionName = firstResult.getJSONObject("AdministrativeArea").getString("LocalizedName");
        this.regionData = cityName + ", " + regionName + ", " + countryName;

        System.out.println("Weather forrrr " + cityName + ", " + regionName + ", " + countryName + ":");
        return locationKey;
    }

    public String getRegionData() {
        return this.regionData;
    }

    private static void getCurrentConditions(String locationKey) throws Exception {
        String url = "http://dataservice.accuweather.com/currentconditions/v1/" + locationKey
                + "?apikey=" + API_KEY + "&language=it&details=true";

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
        JSONObject obj = arr.getJSONObject(0);

        double temp = obj.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value");
        String weatherText = obj.getString("WeatherText");
        double windSpeed = obj.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value");

        System.out.println("\n== Current Conditions ==");
        System.out.println("Weather: " + weatherText);
        System.out.println("Temperature: " + temp + " °C");
        System.out.println("Wind Speed: " + windSpeed + " km/h");
    }

    // old
    private static void getDailyForecastSium(String locationKey) throws Exception {
        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/1day/" + locationKey
                + "?apikey=" + API_KEY + "&language=it&details=true&metric=true";

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

        JSONObject obj = new JSONObject(content.toString());
        JSONObject forecast = obj.getJSONArray("DailyForecasts").getJSONObject(0);

        double minTemp = forecast.getJSONObject("Temperature").getJSONObject("Minimum").getDouble("Value");
        double maxTemp = forecast.getJSONObject("Temperature").getJSONObject("Maximum").getDouble("Value");

        double dayWind = forecast.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
        double nightWind = forecast.getJSONObject("Night").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");

        double hoursOfSun = forecast.getDouble("HoursOfSun");
        double dayPrecip = forecast.getJSONObject("Day").getDouble("HoursOfPrecipitation");
        double nightPrecip = forecast.getJSONObject("Night").getDouble("HoursOfPrecipitation");

        System.out.println("\n== Daily Forecast ==");
        System.out.println("Max/Min Temp: " + maxTemp + "/" + minTemp + " °C");
        System.out.println("Wind: " + dayWind + " km/h");
        System.out.println("Day/Night Hours of Precipitation: " + dayPrecip + "/" + nightPrecip);
    }

    // old
    private static void getFiveDayForecast(String locationKey) throws Exception {
        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/" + locationKey
                + "?apikey=" + API_KEY + "&language=en&details=true&metric=true";

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

        JSONObject forecast = new JSONObject(content.toString());
        JSONArray dailyForecasts = forecast.getJSONArray("DailyForecasts");

        System.out.println("\n== 5 Days Forecast ==");

        for (int i = 1; i < dailyForecasts.length(); i++) {
            JSONObject day = dailyForecasts.getJSONObject(i);
            String date = day.getString("Date").split("T")[0];

            JSONObject temp = day.getJSONObject("Temperature");
            double min = temp.getJSONObject("Minimum").getDouble("Value");
            double max = temp.getJSONObject("Maximum").getDouble("Value");

            String dayPhrase = day.getJSONObject("Day").getString("IconPhrase");
            // String nightPhrase = day.getJSONObject("Night").getString("IconPhrase");

            double dayWind = day.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
            double nightWind = day.getJSONObject("Night").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
            int windDir = day.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Direction").getInt("Degrees");

            System.out.println(date + ": Max/Min: " + max + "/" + min + "°C | Wind: " + dayWind + "km/h " + windDir + " | Weather: " + dayPhrase);
        }
    }

    public CurrentWeatherData getCurrentConditions2(String locationKey) throws Exception {
        String url = "http://dataservice.accuweather.com/currentconditions/v1/" + locationKey
                + "?apikey=" + API_KEY + "&language=en&details=true";

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
        JSONObject obj = arr.getJSONObject(0);

        double temp = obj.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value");
        String weatherText = obj.getString("WeatherText");
        if (weatherText.toLowerCase().contains("clear")) {
            weatherText = "Sunny";
        }
        double windSpeed = obj.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value");
        int windDir = obj.getJSONObject("Wind").getJSONObject("Direction").getInt("Degrees");

        // System.out.println("\n== Current Conditions ==");
        // System.out.println("Weather: " + weatherText);
        // System.out.println("Temperature: " + temp + " °C");
        // System.out.println("Wind Speed: " + windSpeed + " km/h");

        return new CurrentWeatherData(temp, windSpeed, "AccuWeather", weatherText, windDir);

    }

}
