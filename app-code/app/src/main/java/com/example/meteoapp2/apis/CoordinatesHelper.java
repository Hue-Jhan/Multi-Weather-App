package com.example.meteoapp2.apis;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.meteoapp2.MyApp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CoordinatesHelper {

    private static final String API_KEY = "xxx";
    private static final String PREFS_NAME = "weather_prefs";

    public static double[] getCoordinates(String city) throws Exception {
        double[] cached = loadCoordinates(city);
        if (cached != null) {
            return cached;
        }

        String url = "https://geocode.maps.co/search?q=" + city + "&api_key=" + API_KEY;
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
        double lat = Double.parseDouble(firstResult.getString("lat"));
        double lon = Double.parseDouble(firstResult.getString("lon"));
        saveCoordinates(city, lat, lon);
        return new double[]{lat, lon};
    }

    public static double[] loadCoordinates(String city) {
        SharedPreferences prefs = MyApp.getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(city + "_lat") && prefs.contains(city + "_lon")) {
            double lat = Double.longBitsToDouble(prefs.getLong(city + "_lat", 0));
            double lon = Double.longBitsToDouble(prefs.getLong(city + "_lon", 0));
            return new double[]{lat, lon};
        }
        return null;
    }

    public static void saveCoordinates(String city, double lat, double lon) {
        SharedPreferences prefs = MyApp.getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(city + "_lat", Double.doubleToRawLongBits(lat));
        editor.putLong(city + "_lon", Double.doubleToRawLongBits(lon));
        editor.apply();
    }
}
