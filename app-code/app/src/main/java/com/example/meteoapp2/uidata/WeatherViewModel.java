package com.example.meteoapp2.uidata;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Application;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.meteoapp2.datixd.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class WeatherViewModel extends AndroidViewModel {
    private final WeatherRepository repository = new WeatherRepository();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<CurrentWeatherData> weatherDataList = new MutableLiveData<>();
    private final Map<String, CurrentWeatherData> cache = new HashMap<>();
    public LiveData<CurrentWeatherData> getWeatherDataList() {
        return weatherDataList;
    }

    private final MutableLiveData<List<DailyWeatherData>> dailyWeatherDataList = new MutableLiveData<>();
    private final Map<String, List<DailyWeatherData>> dailyCache = new HashMap<>();
    private final Map<String, Long> dailyCacheTimestamps = new HashMap<>();
    public LiveData<List<DailyWeatherData>> getDailyWeatherDataList() {
        return dailyWeatherDataList;
    }

    private List<DailyWeatherData> cachedDailyData = null;
    private long lastDailyFetchTime = 0;

    private SharedPreferences prefs;
    private Gson gson = new Gson();


    public void loadWeather(String city) {
        if (cache.containsKey(city)) {
            weatherDataList.postValue(cache.get(city));
            return;
        }
        executor.execute(() -> {
            try {
                CurrentWeatherData dataList = repository.getCurrentWeatherFromAll(city);
                cache.put(city, dataList);
                weatherDataList.postValue(dataList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void loadDailyWeather(String city) {
        long now = System.currentTimeMillis();
        long twoHoursMillis = 2 * 60 * 60 * 1000;

        if (dailyCache.containsKey(city)) {
            long lastFetch = dailyCacheTimestamps.getOrDefault(city, 0L);
            if ((now - lastFetch) < twoHoursMillis) {
                System.out.println("Daily data loaded from cache for " + city);
                dailyWeatherDataList.postValue(dailyCache.get(city));
                return;
            }
        }

        executor.execute(() -> {
            try {
                List<DailyWeatherData> dailyData = repository.getDailyWeatherFromAll(city);
                dailyCache.put(city, dailyData);
                dailyCacheTimestamps.put(city, System.currentTimeMillis());
                dailyWeatherDataList.postValue(dailyData);
                saveDailyCacheToPrefs(city, dailyData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public WeatherViewModel(Application application) {
        super(application);
        prefs = application.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
        loadDailyCacheFromPrefs();
    }

    private void loadDailyCacheFromPrefs() {
        Map<String, ?> allPrefs = prefs.getAll();

        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith("daily_cache_time_")) {
                String city = key.substring("daily_cache_time_".length());
                System.out.println("city = " + city);
                if (value instanceof Long) {
                    Log.d("WeatherViewModel", "Loading cache...");
                    dailyCacheTimestamps.put(city, (Long) value);
                    Log.d("WeatherViewModel", "Prefs size: " + allPrefs.size());
                }
            } else if (key.startsWith("daily_cache_")) {
                String city = key.substring("daily_cache_".length());
                if (value instanceof String) {
                    String json = (String) value;
                    Type type = new TypeToken<List<DailyWeatherData>>() {}.getType();
                    List<DailyWeatherData> cachedList = gson.fromJson(json, type);
                    Log.d("WeatherViewModel", "Loaded city: " + city + ", entries: " + cachedList.size());
                    dailyCache.put(city, cachedList);
                }
            }
        }
    }

    private void saveDailyCacheToPrefs(String city, List<DailyWeatherData> dailyData) {
        String json = gson.toJson(dailyData);
        prefs.edit()
                .putString("daily_cache_" + city, json)
                .putLong("daily_cache_time_" + city, System.currentTimeMillis())
                .apply();
    }

}