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

public class WeatherApiCom implements WeatherAPI {

    @Override
    public void getAllWeather(String city) throws Exception {
        try {
            String location = city.trim().replace(" ", "%20");

            String apiKey = "xxx";
            String urlString = "http://api.weatherapi.com/v1/forecast.json?key=" + apiKey +
                    "&q=" + location + "&days=3&aqi=no&alerts=no";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());

            JSONObject loc = json.getJSONObject("location");
            JSONObject current = json.getJSONObject("current");

            System.out.println("\n[WeatherAPI.com]");
            System.out.println("Location: " + loc.getString("name") + ", " + loc.getString("region") + ", " + loc.getString("country"));
            System.out.println("Local Time: " + loc.getString("localtime"));
            System.out.println("Temperature: " + current.getDouble("temp_c") + "°C");
            System.out.println("Weather: " + current.getJSONObject("condition").getString("text"));
            System.out.println("Wind: " + current.getDouble("wind_kph") + " km/h " + current.getString("wind_dir"));
            System.out.println();

            JSONArray forecastDays = json.getJSONObject("forecast").getJSONArray("forecastday");
            for (int i = 0; i < forecastDays.length(); i++) {
                JSONObject day = forecastDays.getJSONObject(i);
                String date = day.getString("date");
                JSONObject dayInfo = day.getJSONObject("day");

                System.out.println("Date: " + date);
                System.out.println(" - Max/Min Temp: " + dayInfo.getDouble("maxtemp_c") + "/" + dayInfo.getDouble("mintemp_c") + "°C");
                System.out.println(" - Weather: " + dayInfo.getJSONObject("condition").getString("text"));
                System.out.println(" - Chance of Rain: " + dayInfo.getInt("daily_chance_of_rain") + "%");
                System.out.println(" - Wind: " + dayInfo.getDouble("maxwind_kph") + " km/h");
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        try {
            String location = city.trim().replace(" ", "%20");
            String apiKey = "xxx";
            String urlString = "http://api.weatherapi.com/v1/forecast.json?key=" + apiKey +
                    "&q=" + location + "&days=3&aqi=no&alerts=no";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);     }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONObject loc = json.getJSONObject("location");
            JSONObject current = json.getJSONObject("current");

            double temp = current.getDouble("temp_c");
            String weatherDesc = current.getJSONObject("condition").getString("text");
            if (weatherDesc.toLowerCase().contains("clear")) {
                weatherDesc = "sunny";
            }
            double wind = current.getDouble("wind_kph");
            int windDeg = current.getInt("wind_degree");

            // System.out.println("\n[WeatherAPI.com]");
            // System.out.println("Location: " + loc.getString("name") + ", " + loc.getString("region") + ", " + loc.getString("country"));
            // System.out.println("Local Time: " + loc.getString("localtime"));
            // System.out.println("Temperature: " + current.getDouble("temp_c") + "°C");
            // System.out.println("Weather: " + current.getJSONObject("condition").getString("text"));
            // System.out.println("Wind: " + current.getDouble("wind_kph") + " km/h " + current.getString("wind_dir"));
            // System.out.println();

            return new CurrentWeatherData(temp, wind, "WeatherApi", weatherDesc, windDeg);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        List<DailyWeatherData> result = new ArrayList<>();
        try {
            String location = city.trim().replace(" ", "%20");
            String apiKey = "xxx";
            String urlString = "http://api.weatherapi.com/v1/forecast.json?key=" + apiKey +
                    "&q=" + location + "&days=3&aqi=no&alerts=no";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray forecastDays = json.getJSONObject("forecast").getJSONArray("forecastday");

            for (int i = 0; i < forecastDays.length(); i++) {
                JSONObject day = forecastDays.getJSONObject(i);
                String date = day.getString("date");
                JSONObject dayInfo = day.getJSONObject("day");

                double maxTemp = dayInfo.getDouble("maxtemp_c");
                double minTemp = dayInfo.getDouble("mintemp_c");
                double maxWind = dayInfo.getDouble("maxwind_kph");
                String condition = dayInfo.getJSONObject("condition").getString("text");

                JSONArray hours = day.getJSONArray("hour");
                double totalSin = 0, totalCos = 0;
                int count = 0;

                List<HourlyWeatherData> hourlyList = new ArrayList<>();
                for (int j = 0; j < hours.length(); j++) {
                    JSONObject hour = hours.getJSONObject(j);

                    String time = hour.getString("time").replace(" ", "T");
                    double tempC = hour.getDouble("temp_c");
                    double windKph = hour.getDouble("wind_kph");
                    int windDeg = hour.getInt("wind_degree");
                    String hourCondition = hour.getJSONObject("condition").getString("text");
                    if (hourCondition.equalsIgnoreCase("clear")) {
                        hourCondition = "sunny";
                    }

                    HourlyWeatherData hwd = new HourlyWeatherData(time, tempC, windKph, windDeg, hourCondition, "WeatherApi");
                    hourlyList.add(hwd);

                    double rad = Math.toRadians(windDeg);
                    totalSin += Math.sin(rad);
                    totalCos += Math.cos(rad);
                    count++;
                }

                /*
                for (int j = 0; j < hours.length(); j++) {
                    int windDegree = hours.getJSONObject(j).getInt("wind_degree");
                    double rad = Math.toRadians(windDegree);
                    totalSin += Math.sin(rad);
                    totalCos += Math.cos(rad);
                    count++;
                }
                 */

                int avgWindDir = -1;
                if (count > 0) {
                    double avgRad = Math.atan2(totalSin / count, totalCos / count);
                    if (avgRad < 0) avgRad += 2 * Math.PI;
                    avgWindDir = (int) Math.round(Math.toDegrees(avgRad));
                }

                DailyWeatherData dwd = new DailyWeatherData(
                        date, maxTemp, minTemp, maxWind, avgWindDir, condition,
                        "WeatherApi", hourlyList);
                result.add(dwd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
