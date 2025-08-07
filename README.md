# Multi Weather App
Android Weather App in java that compares multiple Weather APIs/Scrapers to provide the best possible result, and shows daily/hourly data from each API.

# 📱 App & Gui
The empty app is a simple white page with 2 buttons, the "search" button on top right corner will pop up a text menu, once the user inserts a city name in it the APIs will fetch weather data for that location and display it to the user.

The "plus" button on bottom right will show 2 more buttons, one to delete the current location from the location list, the other for a custom useless notification and a useless pop up menu. You are free to customize it if u want.

The user can add infinite locations and swipe through them, the user can also scroll through the scrollMenus used to dispay Daily and Hourly data. Clicking on each day will pop up its hourly data.

Because weather stations are generally not very accurate, it's best not to consider forecasts data for days past the "day after tomorrow". 

# 💻 Code & Apis

The APIs i used are:

- GeoCodeMaps Api ([here](https://geocode.maps.co/));
- VisualCrossing Api ([here](https://www.visualcrossing.com/weather-query-builder/)), international;
- AccuWeather Api, ([here](https://developer.accuweather.com/)), international but requires location key from each city, which requires its Nation code, because of this i set this Api for italian cities only;
- OpenMeteo Api ([here](https://open-meteo.com/en/docs)), international;
- WeatherApi.com Api ([here](https://www.weatherapi.com/)), international;
- IlMeteo (scraper, [here](http://ilmeteo.it/meteo/)), only works for Italy and some general european locations;
- Windy (work in progress Api, [here](https://api.windy.com/)), international.

The code makes requests to the APIs to extract the current weather data, and the forecast data for the next few days, including hourly data. The weather informations are then saved into custom data types, an average result is calculated and displayed to the user. The daily data is saved into storage and updated every 2 hours for efficiency (and to not waste api requests).

