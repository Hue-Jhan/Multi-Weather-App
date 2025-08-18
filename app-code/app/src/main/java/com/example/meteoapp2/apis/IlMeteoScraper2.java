package com.example.meteoapp2.apis;
import com.example.meteoapp2.datixd.CurrentWeatherData;
import com.example.meteoapp2.datixd.DailyWeatherData;
import com.example.meteoapp2.datixd.HourlyWeatherData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IlMeteoScraper2 implements WeatherAPI {

    @Override
    public void getAllWeather(String city) throws Exception {
        String formattedCity = city.toLowerCase().replace(" ", "+");
        String baseUrl = "https://www.ilmeteo.it";

        Document doc = Jsoup.connect(baseUrl + "/meteo/" + formattedCity).get();
        Elements days = doc.select(".forecast_day_selector__list__item");

        for (int i = 1; i < days.size() - 1; i++) {
            Element day = days.get(i);
            Element linkElement = day.selectFirst("a");
            Element conditionElement = day.selectFirst(".s-small-container-all");
            Element dateElement = day.selectFirst(".forecast_day_selector__list__item__link__date");
            Element minTempElement = day.selectFirst(".forecast_day_selector__list__item__link__values__lower");
            Element maxTempElement = day.selectFirst(".forecast_day_selector__list__item__link__values__higher");

            if (conditionElement != null) {
                Element simboloElement = conditionElement.selectFirst("[data-simbolo]");
                if (simboloElement != null) {
                    String simboloStr = simboloElement.attr("data-simbolo");
                    int simboloCode = Integer.parseInt(simboloStr);
                    String description = getWeatherDescription(simboloCode);
                    // System.out.println("Weather Code: " + simboloCode);
                    System.out.println("Weather: " + description);
                }
            }

            if (linkElement != null && dateElement != null) {
                String link = linkElement.attr("href");
                String date = dateElement.text();
                String minTemp = minTempElement != null ? minTempElement.text() : "?";
                String maxTemp = maxTempElement != null ? maxTempElement.text() : "?";

                String dayUrl;
                if (link.startsWith("http")) {
                    dayUrl = link;
                } else {
                    dayUrl = baseUrl + link;
                }

                Document docDay = Jsoup.connect(dayUrl).get();
                Elements rows = docDay.select("tr.forecast_1h, tr.forecast_3h");

                System.out.println("\nDate: " + date);
                System.out.println("Max/Min Temp: " + maxTemp + "/" + minTemp);

                for (Element row : rows) {
                    Elements tds = row.select("td");
                    if (tds.size() > 5) {
                        String hour = tds.get(0).text();
                        String temp = tds.get(2).selectFirst("span.temp_cf").text();
                        Element windTd = tds.get(5);

                        Element directionElement = windTd.selectFirst("abbr");
                        String direction = directionElement != null ? directionElement.ownText() : "?";

                        Element speedElement = windTd.selectFirst("span.boldval.wind_kmkn");
                        String speed = speedElement != null ? speedElement.text() : "0";

                        double speed2;
                        try {
                            speed2 = Double.parseDouble(speed);
                        } catch (NumberFormatException e) {
                            speed2 = 0;
                        }

                        Element gustElement = windTd.selectFirst("span.descri abbr.wind_kmkn");
                        String gust = gustElement != null ? gustElement.text() : "?";

                        Elements descriSpans = windTd.select("span.descri");
                        String strength = descriSpans.size() > 0 ? descriSpans.last().text() : "?";

                        System.out.println(hour + ":00 -> | T:" + temp + "° | Wind: " + direction + " " + String.format("%.1f", (speed2 * 1.8)) + "/" + gust + " kmh (" + strength + ")");
                    }
                }
            }
        }
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        boolean isNight = false;
        String formattedCity = city.toLowerCase().replace(" ", "+");
        String baseUrl = "https://www.ilmeteo.it";
        Document doc = Jsoup.connect(baseUrl + "/meteo/" + formattedCity).get();
        Elements days = doc.select(".forecast_day_selector__list__item");

        if (days.size() <= 1) {
            throw new Exception("No forecast days found.");
        }

        Element today = days.get(1);
        Element linkElement = today.selectFirst("a");
        Element conditionElement = today.selectFirst(".s-small-container-all");

        String description = "null";
        /*if (conditionElement != null) {
            Element simboloElement = conditionElement.selectFirst("[data-simbolo]");
            if (simboloElement != null) {
                String simboloStr = simboloElement.attr("data-simbolo");
                int simboloCode = Integer.parseInt(simboloStr);
                System.out.println("codeeee = "+ simboloCode);
                description = getWeatherDescription(simboloCode);
                System.out.println("codeeee = "+ simboloCode);
                if (description.contains("Night")) {
                    isNight = true;
                }
                // System.out.println("Weather Code: " + simboloCode);
                //System.out.println("Weather: " + description);
            }
        }
         */

        if (linkElement == null) {
            throw new Exception("No link for today found.");
        }

        String link = linkElement.attr("href");
        String dayUrl = link.startsWith("http") ? link : baseUrl + link;
        Document docDay = Jsoup.connect(dayUrl).get();
        Elements rows = docDay.select("tr.forecast_1h, tr.forecast_3h");

        if (rows.isEmpty()) {
            throw new Exception("No hourly rows found.");
        }

        Element row = rows.get(0);
        Elements tds = row.select("td");

        double temperature = 0;
        double windSpeed = 0;
        int windDir = -10; // default
        int simbolo = -1; // fallback

        try {
            Element simboloSpan = row.selectFirst("span[data-simbolo]");
            if (simboloSpan != null) {
                simbolo = Integer.parseInt(simboloSpan.attr("data-simbolo"));
            }
            // System.out.println("cccccode = "+ simbolo);
            description = getWeatherDescription(simbolo);
            // System.out.println("ccccodeeee = "+ simbolo);
            if (description.contains("Night")) {
                isNight = true;
            }
            if (tds.size() > 5) {
                try {
                    String tempText = tds.get(2).selectFirst("span.temp_cf").text().replace("°", "").trim();
                    temperature = Double.parseDouble(tempText);
                } catch (Exception e) {
                    System.out.println("Could not parse temperature");
                }

                try {
                    Element windTd = tds.get(5);
                    Element speedElement = windTd.selectFirst("span.boldval.wind_kmkn");
                    // double windSpeed = 0;
                    if (speedElement == null) {
                        throw new Exception("Could not find wind speed span");
                    }
                    String speed = speedElement.text();
                    windSpeed = Double.parseDouble(speed);
                } catch (Exception e) {
                    System.out.println("Could not parse wind speed");
                }


                try {
                    Element windDirTd = tds.get(4);
                    Element windDiv = windDirTd.selectFirst("div.w[class*=wind]");
                    if (windDiv == null) {
                        throw new Exception("Could not find wind direction div");
                    }
                    String styleAttr = windDiv.attr("style");
                    Matcher matcher = Pattern.compile("rotate\\((\\d+(?:\\.\\d+)?)deg\\)").matcher(styleAttr);
                    if (matcher.find()) {
                        windDir = (int) Double.parseDouble(matcher.group(1));
                    }
                } catch (Exception e) {
                    System.out.println("Could not parse wind direction");
                }
            } else {
                System.out.println("Not enough td elements");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new CurrentWeatherData(temperature, windSpeed, "IlMeteo", description, windDir, isNight);
    }

    // wind n directions are the first hour pick, not average
    /*@Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        String formattedCity = city.toLowerCase().replace(" ", "+");
        String baseUrl = "https://www.ilmeteo.it";
        Document doc = Jsoup.connect(baseUrl + "/meteo/" + formattedCity).get();
        Elements days = doc.select(".forecast_day_selector__list__item");

        if (days.size() <= 1) {
            throw new Exception("No forecast days found."); }

        List<DailyWeatherData> result = new ArrayList<>();
        for (int i = 1; i < days.size(); i++) {
            try {
                Element day = days.get(i);
                Element linkElement = day.selectFirst("a");
                Element conditionElement = day.selectFirst(".s-small-container-all");

                String description = "null";
                int simboloCode = -1;

                if (conditionElement != null) {
                    Element simboloElement = conditionElement.selectFirst("[data-simbolo]");
                    if (simboloElement != null) {
                        String simboloStr = simboloElement.attr("data-simbolo");
                        simboloCode = Integer.parseInt(simboloStr);
                        description = getWeatherDescription(simboloCode);
                    }
                }

                if (linkElement == null) {
                    System.out.println("Skipping day " + i + ": no link.");
                    continue;
                }

                String link = linkElement.attr("href");
                String dayUrl = link.startsWith("http") ? link : baseUrl + link;

                String linkText = linkElement.text().trim(); // e.g. "Lun 21 24° 33°"
                String[] parts = linkText.split(" ");
                if (parts.length < 4) {
                    System.out.println("Skipping day " + i + ": could not split link text properly.");
                    continue;
                }
                String date = (parts[0] + " " + parts[1]); // Lun 21
                String tempMinStr = parts[2].replace("°", "");
                String tempMaxStr = parts[3].replace("°", "");
                double tempMin = Double.parseDouble(tempMinStr);
                double tempMax = Double.parseDouble(tempMaxStr);

                Document docDay = Jsoup.connect(dayUrl).get();
                Elements rows = docDay.select("tr.forecast_1h, tr.forecast_3h");

                if (rows.isEmpty()) {
                    System.out.println("Skipping day " + i + ": no hourly rows.");
                    continue;
                }

                double windSpeed = 0;
                int windDir = -1;

                Element row = rows.get(0);
                Elements tds = row.select("td");
                if (tds.size() > 5) {
                    try {
                        Element windTd = tds.get(5);
                        Element speedElement = windTd.selectFirst("span.boldval.wind_kmkn");
                        if (speedElement != null) {
                            windSpeed = Double.parseDouble(speedElement.text());
                        }

                        Element windDirTd = tds.get(4);
                        Element windDiv = windDirTd.selectFirst("div.w.wind2h, div.w.wind1h");
                        if (windDiv != null) {
                            String styleAttr = windDiv.attr("style");
                            Matcher matcher = Pattern.compile("rotate\\((\\d+(?:\\.\\d+)?)deg\\)").matcher(styleAttr);
                            if (matcher.find()) {
                                windDir = (int) Double.parseDouble(matcher.group(1));
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Could not parse wind: " + e.getMessage());
                    }
                }

                DailyWeatherData dwd = new DailyWeatherData(
                        date, tempMax, tempMin, windSpeed, windDir, description, "IlMeteo"
                );
                result.add(dwd);

            } catch (Exception e) {
                System.out.println("Skipping day due to error: " + e.getMessage());
            }
        }

        return result;
    }


     */

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        String formattedCity = city.toLowerCase().replace(" ", "+");
        String baseUrl = "https://www.ilmeteo.it";
        Document doc = Jsoup.connect(baseUrl + "/meteo/" + formattedCity).get();
        Elements days = doc.select(".forecast_day_selector__list__item");

        if (days.size() <= 1) {
            throw new Exception("No forecast days found.");
        }

        List<DailyWeatherData> result = new ArrayList<>();
        for (int i = 1; i < days.size(); i++) {
            boolean isNight = false;
            try {
                Element day = days.get(i);
                Element linkElement = day.selectFirst("a");
                Element conditionElement = day.selectFirst(".s-small-container-all");

                String description = "null";
                int simboloCode = -1;

                if (conditionElement != null) {
                    Element simboloElement = conditionElement.selectFirst("[data-simbolo]");
                    if (simboloElement != null) {
                        String simboloStr = simboloElement.attr("data-simbolo");
                        simboloCode = Integer.parseInt(simboloStr);
                        description = getWeatherDescription(simboloCode);
                    }
                    if (description.contains("Night")) {
                        isNight = true;
                    }
                }

                if (linkElement == null) {
                    System.out.println("Skipping day " + i + ": no link.");
                    continue;
                }

                String link = linkElement.attr("href");
                String dayUrl = link.startsWith("http") ? link : baseUrl + link;

                String linkText = linkElement.text().trim(); // e.g. "Lun 21 24° 33°"
                String[] parts = linkText.split(" ");
                if (parts.length < 4) {
                    System.out.println("Skipping day " + i + ": could not split link text properly.");
                    continue;
                }
                String rawDate = parts[0] + " " + parts[1];
                String formattedDate = parseIlMeteoDateAuto(rawDate);
                String tempMinStr = parts[2].replace("°", "");
                String tempMaxStr = parts[3].replace("°", "");
                double tempMin = Double.parseDouble(tempMinStr);
                double tempMax = Double.parseDouble(tempMaxStr);

                List<HourlyWeatherData> hourlyList = new ArrayList<>();
                Document docDay = Jsoup.connect(dayUrl).get();
                Elements rows = docDay.select("tr.forecast_1h, tr.forecast_3h:not(.hidden)");

                if (rows.isEmpty()) {
                    System.out.println("Skipping day " + i + ": no hourly rows.");
                    continue;
                }

                LocalDate localDate = LocalDate.parse(formattedDate, DateTimeFormatter.ISO_LOCAL_DATE);
                double totalWindSpeed = 0;
                double totalDirSin = 0;
                double totalDirCos = 0;
                int count = 0;

                for (Element row : rows) {
                    Elements tds = row.select("td");
                    if (tds.size() > 5) {
                        try {
                            String hourStr = row.attr("data-hour");
                            int hourInt = Integer.parseInt(hourStr);
                            String dateTime = String.format("%sT%02d:00", localDate, hourInt);

                            Element tempEl = row.selectFirst("span.temp_cf");
                            double temperature = Double.parseDouble(tempEl.text().replace(",", "."));

                            Element symbolEl = row.selectFirst("span.s-small");
                            int simbolo = Integer.parseInt(symbolEl.attr("data-simbolo"));
                            String weatherDesc = getWeatherDescription(simbolo);

                            Element windTd = tds.get(5);
                            Element speedElement = windTd.selectFirst("span.boldval.wind_kmkn");
                            double windSpeed = 0;
                            if (speedElement != null) {
                                windSpeed = Double.parseDouble(speedElement.text());
                                totalWindSpeed += windSpeed;
                            }

                            int dirDeg = -1;
                            try {
                                Element windDirTd = tds.get(4);
                                Element windDiv = windDirTd.selectFirst("div.w[class*=wind]");
                                if (windDiv == null) {
                                    throw new Exception("Could not find wind direction div");
                                }
                                String styleAttr = windDiv.attr("style");
                                Matcher matcher = Pattern.compile("rotate\\((\\d+(?:\\.\\d+)?)deg\\)").matcher(styleAttr);
                                if (matcher.find()) {
                                        dirDeg = (int) Double.parseDouble(matcher.group(1));
                                        double dirRad = Math.toRadians(dirDeg);
                                        totalDirSin += Math.sin(dirRad);
                                        totalDirCos += Math.cos(dirRad);
                                    }
                            } catch (Exception e) {
                                System.out.println("Could not parse wind direction");
                            }
                            count++;

                            HourlyWeatherData hour = new HourlyWeatherData(
                                    dateTime, temperature, windSpeed, dirDeg, weatherDesc, "IlMeteo");
                            hourlyList.add(hour);

                        } catch (Exception e) {
                            System.out.println("Could not parse row: " + e.getMessage());
                        }
                    }
                }

                double avgWindSpeed = count > 0 ? totalWindSpeed / count : 0;
                double avgWindDir = -1;
                if (count > 0) {
                    double avgSin = totalDirSin / count;
                    double avgCos = totalDirCos / count;
                    double avgRad = Math.atan2(avgSin, avgCos);
                    if (avgRad < 0) {
                        avgRad += 2 * Math.PI;
                    }
                    avgWindDir = Math.toDegrees(avgRad);
                    avgWindDir = (avgWindDir + 180) % 360;
                }
                avgWindSpeed = Math.round(avgWindSpeed * 10.0) / 10.0;

                DailyWeatherData dwd = new DailyWeatherData(
                        formattedDate, tempMax, tempMin, avgWindSpeed, (int) avgWindDir, description, "IlMeteo"
                );
                dwd.hourlyData.addAll(hourlyList);
                result.add(dwd);

            } catch (Exception e) {
                System.out.println("Skipping day due to error: " + e.getMessage());
            }
        }
        return result;
    }


    public static String parseIlMeteoDateAuto(String dayStr) {
        String[] parts = dayStr.split(" ");
        if (parts.length != 2) return null;

        String dow = parts[0].trim();
        int dayOfMonth = Integer.parseInt(parts[1]);

        LocalDate today = LocalDate.now();

        int year = today.getYear();
        int month = today.getMonthValue();

        if (dayOfMonth < today.getDayOfMonth() - 10) {
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        LocalDate date = LocalDate.of(year, month, dayOfMonth);
        DayOfWeek actualDow = date.getDayOfWeek();
        String dowShort = getItalianShortDayOfWeek(actualDow);

        if (!dowShort.equalsIgnoreCase(dow)) {
            System.out.println("Warning: parsed DOW does not match! " + dow + " vs " + dowShort);
        }

        return date.toString();
    }


    public static String getItalianShortDayOfWeek(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Lun";
            case TUESDAY: return "Mar";
            case WEDNESDAY: return "Mer";
            case THURSDAY: return "Gio";
            case FRIDAY: return "Ven";
            case SATURDAY: return "Sab";
            case SUNDAY: return "Dom";
            default: return "?";
        }
    }

    private static String getWeatherDescription(int code) {
        boolean isNight = false;

        if (code >= 100) {
            isNight = true;
            code -= 100;
        }

        String description;

        if (code == 1) {
            description = "Sunny";
        } else if (code == 2) {
            description = "Mostly Sunny";
        } else if (code == 3) {
            description = "Partly Cloudy";
        } else if (code == 4 || code == 8) {
            description = "Cloudy";
        } else if (code >= 5 && code <= 9) {
            description = "Rain";
        } else if (code >= 10 && code <= 12) {
            description = "Showers";
        } else if (code >= 13 && code <= 19) {
            description = "Thunderstorm";
        } else {
            description = "Unknown";
        }

        if (isNight) {
            description += " Night";
        }

        return description;
    }


}
