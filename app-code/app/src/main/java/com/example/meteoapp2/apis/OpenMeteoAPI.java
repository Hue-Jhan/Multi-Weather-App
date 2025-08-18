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

public class OpenMeteoAPI implements WeatherAPI {

    private static final String API_KEY = "xxx";

    @Override
    public void getAllWeather(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return;
        }

        // getCurrentWeather(coordinates[0], coordinates[1]);
        // getDailyWeather(coordinates[0], coordinates[1]);
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return null;
        }
        return getCurrentWeather2(coordinates[0], coordinates[1]);
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return List.of();
        }
        double lat = coordinates[0];
        double lon = coordinates[1];

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&daily=temperature_2m_max,temperature_2m_min,weather_code,rain_sum,wind_speed_10m_max,wind_direction_10m_dominant"
                + "&hourly=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m"
                + "&timezone=Europe%2FBerlin"
                + "&past_days=1"
                + "&forecast_days=4";

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
        JSONObject daily = obj.getJSONObject("daily");
        JSONArray dates = daily.getJSONArray("time");
        JSONArray tempsMax = daily.getJSONArray("temperature_2m_max");
        JSONArray tempsMin = daily.getJSONArray("temperature_2m_min");
        JSONArray weatherCodes = daily.getJSONArray("weather_code");
        JSONArray windSpeeds = daily.getJSONArray("wind_speed_10m_max");
        JSONArray windDirs = daily.getJSONArray("wind_direction_10m_dominant");

        JSONObject hourly = obj.getJSONObject("hourly");
        JSONArray timesHourly = hourly.getJSONArray("time");
        JSONArray tempsHourly = hourly.getJSONArray("temperature_2m");
        JSONArray weatherCodesHourly = hourly.getJSONArray("weather_code");
        JSONArray windSpeedsHourly = hourly.getJSONArray("wind_speed_10m");
        JSONArray windDirsHourly = hourly.getJSONArray("wind_direction_10m");

        List<DailyWeatherData> result = new ArrayList<>();
        for (int i = 0; i < dates.length(); i++) {
            String date = dates.getString(i);
            double tempMax = tempsMax.getDouble(i);
            double tempMin = tempsMin.getDouble(i);
            int weatherCode = weatherCodes.getInt(i);
            double windSpeed = windSpeeds.getDouble(i);
            int windDir = windDirs.getInt(i);

            String description = getWeatherDescription(weatherCode);
            DailyWeatherData dwd = new DailyWeatherData(
                    date, tempMax, tempMin, windSpeed, windDir, description, "OpenMeteo"
            );

            List<HourlyWeatherData> hourlyList = new ArrayList<>();
            for (int j = 0; j < timesHourly.length(); j++) {
                String hourTime = timesHourly.getString(j);
                if (hourTime.startsWith(date)) {
                    double temp = tempsHourly.getDouble(j);
                    int hCode = weatherCodesHourly.getInt(j);
                    double hWindSpeed = windSpeedsHourly.getDouble(j);
                    int hWindDir = windDirsHourly.getInt(j);
                    String desc = getWeatherDescription(hCode);

                    HourlyWeatherData hwd = new HourlyWeatherData(
                            hourTime, temp, hWindSpeed, hWindDir, desc, "OpenMeteo"
                    );
                    hourlyList.add(hwd);
                }
            }
            dwd.hourlyData.addAll(hourlyList);
            result.add(dwd);
        }
        return result;
    }

    public double[] getCoordinates(String city) throws Exception {
        /*String url = "http://dataservice.accuweather.com/locations/v1/cities/IT/search"
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

        String cityName = firstResult.getString("LocalizedName");
        String countryName = firstResult.getJSONObject("Country").getString("LocalizedName");
        String regionName = firstResult.getJSONObject("AdministrativeArea").getString("LocalizedName");
        double latitude = firstResult.getJSONObject("GeoPosition").getDouble("Latitude");
        double longitude = firstResult.getJSONObject("GeoPosition").getDouble("Longitude");

        // System.out.println("\nWeather for " + cityName + ", " + regionName + ", " + countryName + ":");
        return new double[]{latitude, longitude};
         */
        return CoordinatesHelper.getCoordinates(city);
    }

    private String getWeatherDescription(int code) {
        boolean isNight = false;
        if (code >= 100) {
            isNight = true;
            code -= 100;
        }
        String description;
        if (code >= 0 && code <= 3) {
            description = "Sunny";
        } else if (code >= 4 && code <= 5) {
            description = "Partly Cloudy";
        } else if (code >= 6 && code <= 7) {
            description = "Cloudy";
        } else if (code == 10) {
            description = "Mist";
        } else if (code == 11) {
            description = "Fog";
        } else if (code >= 20 && code <= 29) {
            description = "Precipitation";
        } else if (code >= 30 && code <= 35) {
            description = "Dust or Sand";
        } else if (code >= 40 && code <= 49) {
            description = "Fog or Ice Fog";
        } else if (code >= 50 && code <= 55) {
            description = "Drizzle";
        } else if (code >= 56 && code <= 57) {
            description = "Freezing Drizzle";
        } else if (code >= 60 && code <= 65) {
            description = "Rain";
        } else if (code >= 66 && code <= 67) {
            description = "Freezing Rain";
        } else if (code >= 68 && code <= 69) {
            description = "Rain and Snow";
        } else if (code >= 70 && code <= 79) {
            description = "Snow";
        } else if (code >= 80 && code <= 82) {
            description = "Showers";
        } else if (code >= 90 && code <= 95) {
            description = "Thunderstorm";
        } else if (code >= 96 && code <= 99) {
            description = "Thunderstorm with Hail";
        } else {
            description = "Unknown";
        }
        if (isNight) {
            description += " Night";
        }
        return description;

    }

    private void getCurrentWeather(double lat, double lon) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&timezone=Europe%2FBerlin"
                + "&forecast_days=3"
                + "&current=temperature_2m,weather_code,precipitation,rain,wind_speed_10m";

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
        JSONObject current = obj.getJSONObject("current");

        double temp = current.getDouble("temperature_2m");
        double wind = current.getDouble("wind_speed_10m");
        int weatherCode = current.getInt("weather_code");

        System.out.println("Temperature: " + temp + " °C");
        System.out.println("Weather: " + getWeatherDescription(weatherCode));
        System.out.println("Wind: " + wind + " km/h");
    }

    private CurrentWeatherData getCurrentWeather2(double lat, double lon) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&timezone=Europe%2FBerlin"
                + "&forecast_days=3"
                + "&current=temperature_2m,weather_code,precipitation,rain,wind_speed_10m,wind_direction_10m";

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
        JSONObject current = obj.getJSONObject("current");

        double temp = current.getDouble("temperature_2m");
        double wind = current.getDouble("wind_speed_10m");
        int weatherCode = current.getInt("weather_code");
        int winddir = current.getInt("wind_direction_10m");

        return new CurrentWeatherData(temp, wind, "OpenMeteo", getWeatherDescription(weatherCode), winddir);
        //System.out.println("Temperature: " + temp + " °C");
        //System.out.println("Weather: " + getWeatherDescription(weatherCode));
        //System.out.println("Wind: " + wind + " km/h");
    }

    private void getDailyWeather(double lat, double lon) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&daily=temperature_2m_max,temperature_2m_min,weather_code,rain_sum,wind_speed_10m_max"
                + "&timezone=Europe%2FBerlin"
                + "&past_days=1"
                + "&forecast_days=4";

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
        JSONObject daily = obj.getJSONObject("daily");
        JSONArray dates = daily.getJSONArray("time");
        JSONArray tempMax = daily.getJSONArray("temperature_2m_max");
        JSONArray tempMin = daily.getJSONArray("temperature_2m_min");
        JSONArray weatherCode = daily.getJSONArray("weather_code");
        JSONArray windMax = daily.getJSONArray("wind_speed_10m_max");

        System.out.println("\nPast/Next 3 Days Weather:");
        for (int i = 0; i < dates.length(); i++) {
            System.out.println(dates.getString(i) + " -> Max/Min T: "
                    + Math.ceil(tempMax.getDouble(i)) + "/" + Math.ceil(tempMin.getDouble(i)) + "°C"
                    + " | Weather: " + getWeatherDescription(weatherCode.getInt(i))
                    + " | Wind: " + windMax.getDouble(i) + " km/h");
        }
    }
}
