package com.example.meteoapp2.apis;
import com.example.meteoapp2.datixd.CurrentWeatherData;
import com.example.meteoapp2.datixd.DailyWeatherData;

import java.util.List;

public interface WeatherAPI {
    void getAllWeather(String city) throws Exception;
    CurrentWeatherData getCurrentWeather(String city) throws Exception;
    List<DailyWeatherData> getDailyForecast(String city) throws Exception;
}

