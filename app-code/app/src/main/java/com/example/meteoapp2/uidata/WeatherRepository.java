package com.example.meteoapp2.uidata;

import android.util.Log;

import com.example.meteoapp2.R;
import com.example.meteoapp2.apis.*;
import com.example.meteoapp2.apis.WeatherAPI;
import com.example.meteoapp2.datixd.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherRepository {
    private final List<WeatherAPI> apis = new ArrayList<>();
    private static int backgroundCode;

    public WeatherRepository() {
        apis.add(new IlMeteoScraper2());
        // apis.add(new AccuWeatherAPI());
        // apis.add(new VisualCrossingAPI());
        apis.add(new OpenMeteoAPI());
        apis.add(new WeatherApiCom());
    }

    public CurrentWeatherData getCurrentWeatherFromAll(String city) throws Exception {
        List<CurrentWeatherData> collected = new ArrayList<>();
        for (WeatherAPI api : apis) {
            try {
                CurrentWeatherData data = api.getCurrentWeather(city);
                collected.add(data);
            } catch (Exception e) {
                System.out.println("Error: " + api.getClass().getSimpleName());
            }
        }
        if (collected.isEmpty()) {
            Log.e("aa", "No current data");
            return null;     }

        System.out.println(city);
        for (CurrentWeatherData d : collected) {
            System.out.printf("%-15s | Temp: %5.1f°C | Wind: %4.1f kmh %3d° (%s) | %s\n",
                    d.source, d.temperature, d.windSpeed, d.windDir,
                    windDirectionToCompass(d.windDir), d.weatherDescription);   }

        CurrentWeatherData avg = calculateAverage(collected);
        System.out.println("Average Current Weather: "
                + "T: " + String.format("%.1f", avg.temperature) + "°C "
                + " | Wind: " + String.format("%.1f", avg.windSpeed) + "kmh "
                + windDirectionToCompass(avg.windDir)
                + " | " + avg.weatherDescription);
        offsetCheck(collected, avg);
        return avg;
    }

    public List<DailyWeatherData> getDailyWeatherFromAll(String city) throws Exception {
        List<DailyWeatherData> collected = new ArrayList<>();
        for (WeatherAPI api : apis) {
            try {
                List<DailyWeatherData> data = api.getDailyForecast(city);
                collected.addAll(data);
            } catch (Exception e) {
                System.out.println("Error: " + api.getClass().getSimpleName());
            }
        }
        if (collected.isEmpty()) {
            Log.e("aa", "No current data");
            return null;     }

        System.out.println(city);
        String previousSource = "";
        for (DailyWeatherData d : collected) {
            if (!d.source.equals(previousSource)) {
                System.out.println("\n" + d.source + " data:");
                previousSource = d.source;  }

            if (d.windDir < 0) {
                System.out.printf("%-10s | T: %4.1f/%4.1f°C | %skmh  ?  | %s\n",
                        d.date, d.temperature, d.TempMin, d.windSpeed, d.weatherDescription);
            } else {
                System.out.printf("%-10s | T: %4.1f/%4.1f°C | %skmh %-4s | %s\n",
                        d.date, d.temperature, d.TempMin, d.windSpeed, windDirectionToCompass(d.windDir), d.weatherDescription);
            }

            if (d.hourlyData != null && !d.hourlyData.isEmpty()) {
                System.out.println("  Hourly:");
                for (HourlyWeatherData h : d.hourlyData) {
                    String timeOnly = h.dateTime.split("T")[1].split(":00")[0];
                    String hourFormatted = timeOnly + ":00";
                    System.out.printf("    %s | %.1f°C | %skmh %-4s | %s\n",
                            hourFormatted, h.temperature, h.windSpeed, windDirectionToCompass(h.windDir), h.weatherDescription);
                }  }
        }

        List<DailyWeatherData> avgPerDay = calculateDailyAverages(collected);
        avgPerDay.sort(Comparator.comparing(d -> d.date));
        System.out.println("\nAverage per day:");
        for (DailyWeatherData avg : avgPerDay) {
            System.out.printf("%-10s | T: %.1f/%.1f°C | %.1fkmh %-4s | %s\n",
                    avg.date, avg.temperature, avg.TempMin,
                    avg.windSpeed, windDirectionToCompass(avg.windDir), avg.weatherDescription);

            for (HourlyWeatherData h : avg.hourlyData) {
                String timeOnly = h.dateTime.split("T")[1].split(":00")[0];
                String hourFormatted = timeOnly + ":00";
                System.out.printf("    %s | %.1f°C | %.1fkmh %-4s | %s\n",
                        hourFormatted, h.temperature, h.windSpeed,
                        windDirectionToCompass(h.windDir), h.weatherDescription);
            }
        }
        // dailyOffsetCheck(collected, avgPerDay);
        return avgPerDay;
    }

    static void offsetCheck(List<CurrentWeatherData> data, CurrentWeatherData avg) {
        double tempThreshold = 2.0;  // degrees Celsius difference allowed
        double windThreshold = 5.0;  // km/h difference allowed
        double dirThreshold = 40.0;  // degrees allowed deviation for wind dir
        boolean outlierFlag = false;

        for (CurrentWeatherData d : data) {
            boolean tempOutlier = Math.abs(d.temperature - avg.temperature) > tempThreshold;
            boolean windOutlier = Math.abs(d.windSpeed - avg.windSpeed) > windThreshold;

            boolean dirOutlier = false;
            if (avg.windDir >= 0 && d.windDir >= 0) {
                double diff = Math.abs(d.windDir - avg.windDir);
                // wind direction is circular: 350° vs 10° -> only 20° apart
                diff = Math.min(diff, 360 - diff);
                dirOutlier = diff > dirThreshold;
            }

            if (tempOutlier || windOutlier || dirOutlier) {
                System.out.println("Warning: " + d.source + " has outlier data!");
                outlierFlag = true;
            }
        }
        if (!outlierFlag) {
            // System.out.println("\nNo outlier data");
        }
    }
    static List<DailyWeatherData> calculateDailyAverages(List<DailyWeatherData> data) {
        Map<String, List<DailyWeatherData>> grouped = new HashMap<>();
        for (DailyWeatherData d : data) {
            grouped.computeIfAbsent(d.date, k -> new ArrayList<>()).add(d);
        }

        Map<String, List<HourlyWeatherData>> hourlyMap = calculateHourlyAverages(data);
        List<DailyWeatherData> averages = new ArrayList<>();
        for (String date : grouped.keySet()) {
            List<DailyWeatherData> sameDay = grouped.get(date);

            double sumMax = 0, sumMin = 0, sumWind = 0;

            for (DailyWeatherData d : sameDay) {
                sumMax += d.temperature;
                sumMin += d.TempMin;
                sumWind += d.windSpeed;
            }

            double avgMax = sumMax / sameDay.size();
            double avgMin = sumMin / sameDay.size();
            double avgWind = sumWind / sameDay.size();
            int avgDir = averageDailyWindDirection(sameDay);
            String finalDesc = mergeDailyWeatherDescriptions(sameDay);
            List<HourlyWeatherData> hourly = hourlyMap.getOrDefault(date, new ArrayList<>());

            averages.add(new DailyWeatherData(
                    date, avgMax, avgMin, Math.round(avgWind * 10.0) / 10.0, avgDir, finalDesc, "Average", hourly
            ));
        }
        return averages;
    }
    static CurrentWeatherData calculateAverage(List<CurrentWeatherData> data) {
        double sumTemp = 0;
        double sumWind = 0;
        boolean isNight = false;

        for (CurrentWeatherData d : data) {
            sumTemp += d.temperature;
            sumWind += d.windSpeed;

            if (d.isNight) {
                isNight = true;
            }
        }

        double avgTemp = sumTemp / data.size();
        double avgWind = sumWind / data.size();
        int avgWindDir = averageWindDirection(data);
        String avgDesc = mergeWeatherDescriptions(data);

        return new CurrentWeatherData(avgTemp, avgWind, "Average", avgDesc, avgWindDir, isNight);
    }
    static int averageWindDirection(List<CurrentWeatherData> data) {
        double sumSin = 0.0;
        double sumCos = 0.0;
        int totalWeight = 0;

        for (CurrentWeatherData d : data) {
            if (d.windDir >= 0) {
                double radians = Math.toRadians(d.windDir);
                int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                sumSin += Math.sin(radians) * weight;
                sumCos += Math.cos(radians) * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) return -10; // no valid data

        double avgRadians = Math.atan2(sumSin / totalWeight, sumCos / totalWeight);
        double avgDegrees = Math.toDegrees(avgRadians);
        if (avgDegrees < 0) avgDegrees += 360;

        return (int) Math.round(avgDegrees);
    }
    static String mergeWeatherDescriptions(List<CurrentWeatherData> data) {
        double total = 0;
        int totalWeight = 0;

        for (CurrentWeatherData d : data) {
            if (d.weatherDescription != null) {
                String desc = d.weatherDescription.toLowerCase().trim();

                Integer score = null;
                for (String key : DESCRIPTION_SCORES.keySet()) {
                    if (desc.contains(key)) {
                        score = DESCRIPTION_SCORES.get(key);
                        break;
                    }
                }

                System.out.println("score: " + score);
                if (score != null) {
                    int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                    total += score * weight;
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight == 0) return "Unknown";
        int avgScore = (int) Math.floor(total / totalWeight);
        System.out.println("avgscore = " + avgScore);
        return scoreToDescription(avgScore);
    }
    static int averageDailyWindDirection(List<DailyWeatherData> data) {
        double sumSin = 0.0;
        double sumCos = 0.0;
        int totalWeight = 0;

        for (DailyWeatherData d : data) {
            if (d.windDir >= 0) {
                double radians = Math.toRadians(d.windDir);
                int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                sumSin += Math.sin(radians) * weight;
                sumCos += Math.cos(radians) * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) return -10; // no valid wind dir

        double avgRadians = Math.atan2(sumSin / totalWeight, sumCos / totalWeight);
        double avgDegrees = Math.toDegrees(avgRadians);
        if (avgDegrees < 0) avgDegrees += 360;

        return (int) Math.round(avgDegrees);
    }
    static String mergeDailyWeatherDescriptions(List<DailyWeatherData> data) {
        double total = 0;
        int totalWeight = 0;

        for (DailyWeatherData d : data) {
            if (d.weatherDescription != null) {
                String desc = d.weatherDescription.toLowerCase().trim();

                Integer score = null;
                for (String key : DESCRIPTION_SCORES.keySet()) {
                    if (desc.contains(key)) {
                        score = DESCRIPTION_SCORES.get(key);
                        break;
                    }
                }

                if (score != null) {
                    int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                    total += score * weight;
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight == 0) return "Unknown";
        int avgScore = (int) Math.floor(total / totalWeight);
        return scoreToDescription(avgScore);
    }

    static Map<String, List<HourlyWeatherData>> calculateHourlyAverages(List<DailyWeatherData> dailyData) {
        Map<String, Map<String, List<HourlyWeatherData>>> grouped = new HashMap<>();

        for (DailyWeatherData day : dailyData) {
            if (day.hourlyData == null) continue;
            for (HourlyWeatherData h : day.hourlyData) {
                String[] parts = h.dateTime.split("T");
                String date = parts[0];
                String hour = parts[1].substring(0, 2); // 00, 01, ..., 23

                grouped
                        .computeIfAbsent(date, k -> new HashMap<>())
                        .computeIfAbsent(hour, k -> new ArrayList<>())
                        .add(h);
            }
        }

        Map<String, List<HourlyWeatherData>> result = new HashMap<>();
        for (String date : grouped.keySet()) {
            List<HourlyWeatherData> averagedPerHour = new ArrayList<>();

            for (String hour : grouped.get(date).keySet()) {
                List<HourlyWeatherData> entries = grouped.get(date).get(hour);

                double sumTemp = 0, sumWind = 0;
                int count = entries.size();

                List<DailyWeatherData> asDaily = new ArrayList<>();
                for (HourlyWeatherData h : entries) {
                    sumTemp += h.temperature;
                    sumWind += h.windSpeed;

                    // Convert to dummy DailyWeatherData to reuse helper functions
                    asDaily.add(new DailyWeatherData(
                            date, 0, 0, h.windSpeed, h.windDir, h.weatherDescription, h.source
                    ));
                }

                double avgTemp = sumTemp / count;
                double avgWind = sumWind / count;
                int avgDir = averageDailyWindDirection(asDaily);
                String avgDesc = mergeDailyWeatherDescriptions(asDaily);

                HourlyWeatherData averaged = new HourlyWeatherData(
                        date + "T" + hour + ":00:00",
                        avgTemp, avgWind, avgDir, avgDesc, "Average"
                );

                averagedPerHour.add(averaged);
            }

            averagedPerHour.sort(Comparator.comparing(h -> h.dateTime));
            result.put(date, averagedPerHour);
        }

        return result;
    }


    static List<DailyWeatherData> calculateDailyAverages2(List<DailyWeatherData> data) {
        Map<String, List<DailyWeatherData>> grouped = new HashMap<>();
        for (DailyWeatherData d : data) {
            grouped.computeIfAbsent(d.date, k -> new ArrayList<>()).add(d);
        }

        Map<String, List<HourlyWeatherData>> hourlyMap = calculateHourlyAverages(data);
        List<DailyWeatherData> averages = new ArrayList<>();
        for (String date : grouped.keySet()) {
            List<DailyWeatherData> sameDay = grouped.get(date);
            double sumMax = 0, sumMin = 0, sumWind = 0;
            double sumDirSin = 0, sumDirCos = 0;
            int windDirCount = 0;

            for (DailyWeatherData d : sameDay) {
                sumMax += d.temperature;
                sumMin += d.TempMin;
                sumWind += d.windSpeed;

                if (d.windDir >= 0) {
                    double rad = Math.toRadians(d.windDir);
                    sumDirSin += Math.sin(rad);
                    sumDirCos += Math.cos(rad);
                    windDirCount++;
                }
            }

            double avgMax = sumMax / sameDay.size();
            double avgMin = sumMin / sameDay.size();
            double avgWind = sumWind / sameDay.size();

            int avgDir = averageDailyWindDirection(sameDay);
            String finalDesc = mergeDailyWeatherDescriptions(sameDay);
            /*
            int avgDir = -1;
            if (windDirCount > 0) {
                double avgSin = sumDirSin / windDirCount;
                double avgCos = sumDirCos / windDirCount;
                double avgRad = Math.atan2(avgSin, avgCos);
                if (avgRad < 0) avgRad += 2 * Math.PI;
                avgDir = (int) Math.round(Math.toDegrees(avgRad));
            }

            String baseDesc = sameDay.get(0).weatherDescription.toLowerCase().trim();
            boolean mismatch = false;
            for (DailyWeatherData d : sameDay) {
                if (!d.weatherDescription.toLowerCase().contains(baseDesc)) {
                    mismatch = true;
                    break;
                }
            }
            String finalDesc = baseDesc;
            if (mismatch) finalDesc += " (+)";
             */

            averages.add(new DailyWeatherData(
                    date, avgMax, avgMin, Math.round(avgWind * 10.0) / 10.0, avgDir, finalDesc, "Average"
            ));
        }
        return averages;
    }
    public static int getBackgroundResourceForCondition(String weatherDescription, boolean isNight) {
        if (weatherDescription == null) return R.drawable.sunny_style2;

        String desc = weatherDescription.toLowerCase().trim();

        switch (desc) {
            case "sunny":
            case "clear":
                return isNight ? R.drawable.night_style2 : R.drawable.sunny_style2;

            case "partly cloudy":
                return isNight ? R.drawable.cloudy_night_style : R.drawable.partly_cloudy_day_style;

            case "cloudy":
            case "mostly cloudy":
            case "overcast":
                return isNight ? R.drawable.cloudy_night_style : R.drawable.cloudy_stylee;

            case "rain":
            case "drizzle":
                return isNight ? R.drawable.rain_style : R.drawable.rain_style;

            case "showers":
                return isNight ? R.drawable.rain_style : R.drawable.rain_style;

            case "thunderstorm":
                return R.drawable.thunderstorm_style;

            case "snow":
                return R.drawable.snow_styled;

            default:
                return R.drawable.sunny_style2;
        }
    }
    private static final Map<String, Integer> DESCRIPTION_SCORES = new HashMap<>();
    static {
        DESCRIPTION_SCORES.put("sunny", -1);
        DESCRIPTION_SCORES.put("clear", -1);
        DESCRIPTION_SCORES.put("partly cloudy", 1);
        DESCRIPTION_SCORES.put("mostly cloudy", 2);
        DESCRIPTION_SCORES.put("cloudy", 2);
        DESCRIPTION_SCORES.put("overcast", 2);
        DESCRIPTION_SCORES.put("rain", 3);
        DESCRIPTION_SCORES.put("drizzle", 3);
        DESCRIPTION_SCORES.put("showers", 4);
        DESCRIPTION_SCORES.put("thunderstorm", 5);
        DESCRIPTION_SCORES.put("snow", 8);
    }

    private static String scoreToDescription(int score) {
        if (score <= 0) return "Sunny";
        if (score == 1) return "Partly cloudy";
        if (score == 2) return "Cloudy";
        if (score == 3) return "Rain";
        if (score == 4) return "Showers";
        if (score == 5) return "Thunderstorm";
        if (score >= 6) return "Snow";
        return "Unknown";
    }
    static String mergeDescriptions(List<String> descs) {
        if (descs.isEmpty()) return "Unknown";

        Map<String, String> clusters = new HashMap<>();
        clusters.put("sun", "Sunny");
        clusters.put("clear", "Sunny");
        clusters.put("cloud", "Cloudy");
        clusters.put("overcast", "Cloudy");
        clusters.put("rain", "Rain");
        clusters.put("shower", "Rain");
        clusters.put("drizzle", "Rain");
        clusters.put("snow", "Snow");
        clusters.put("thunder", "Thunderstorm");
        clusters.put("storm", "Thunderstorm");
        clusters.put("fog", "Fog");
        clusters.put("mist", "Fog");
        clusters.put("haze", "Fog");
        clusters.put("ice", "Fog");
        clusters.put("dust", "Dust");
        clusters.put("sand", "Dust");

        Map<String, Integer> counts = new HashMap<>();

        for (String d : descs) {
            boolean matched = false;
            for (Map.Entry<String, String> e : clusters.entrySet()) {
                if (d.contains(e.getKey())) {
                    counts.put(e.getValue(), counts.getOrDefault(e.getValue(), 0) + 1);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                counts.put(d, counts.getOrDefault(d, 0) + 1);
            }
        }

        String topCluster = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                topCluster = e.getKey();
            }
        }

        boolean mismatch = counts.size() > 1;
        return mismatch ? topCluster + " (+)" : topCluster;
    }

    public static String windDirectionToCompass(double degrees) {
        if (degrees < 0) return "N/A";
        String[] directions = {
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
        };
        int index = (int) Math.round(((degrees % 360) / 22.5));
        return directions[index % 16];
    }
    public static String getRegion(String city) throws Exception {
        String url = "http://dataservice.accuweather.com/locations/v1/cities/IT/search"
                + "?apikey=" + "hz3hoigIWuZYPNggVAkAxPiqmgFOfEMZ"
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
        String regionData = regionName + ", " + countryName;
        return regionData;
    }
}
