# Multi Weather App
Android Weather App in java that compares multiple Weather APIs/Scrapers to provide the best possible result, and shows daily/hourly data from each API.

# 📱 App & Gui
The

# 💻 Code & Apis

The APIs i used are:

- GeoCodeMaps APi ([here](https://geocode.maps.co/));
- VisualCrossing Api ([here](https://www.visualcrossing.com/weather-query-builder/));
- AccuWeather Api ([here](https://developer.accuweather.com/));
- OpenMeteo Api ([here](https://open-meteo.com/en/docs));
- WeatherApi.com Api ([here](https://www.weatherapi.com/));
- IlMeteo (scraper, [here](http://ilmeteo.it/meteo/));
- Windy (work in progress, [here](https://api.windy.com/)).

The code makes requests to the APIs to extract the current weather data, and the forecast data for the next few days, including hourly data. The weather informations are then saved into custom data types, an average result is calculated and displayed to the user.  

