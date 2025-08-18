package com.example.meteoapp2.uidata;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.meteoapp2.R;
import com.example.meteoapp2.datixd.DailyWeatherData;
import com.example.meteoapp2.datixd.HourlyWeatherData;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eightbitlab.com.blurview.BlurView;

public class LocationFragment extends Fragment {

    private static final String ARG_LOCATION = "location";
    private BlurView blurView;
    private TextView locationNameTextView;
    private TextView infoSign;
    private String locationName;
    private TextView regionNameTextView;
    private ImageView weatherIconImageView;
    private TextView temperatureTextView;
    private TextView windInfoTextView;
    private TextView windDirTextView;
    private TextView weatherConditionView;
    private WeatherViewModel weatherViewModel;
    private static final long TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000;

    public static LocationFragment newInstance(String location) {
        LocationFragment fragment = new LocationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        SharedPreferences prefs = requireContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong("daily_update_time_" + locationName, 0);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate > TWO_HOURS_MILLIS) {
            weatherViewModel.loadDailyWeather(locationName);
            // prefs.edit().putLong("daily_update_time_" + locationName, currentTime).apply();
        }

        weatherViewModel.loadWeather(locationName);
        // weatherViewModel.loadDailyWeather(locationName);
        weatherViewModel.getWeatherDataList().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                requireActivity().findViewById(R.id.progressBar).setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            locationName = getArguments().getString(ARG_LOCATION);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location, container, false);

        requireActivity().findViewById(R.id.voipLogoCard2).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        requireActivity().findViewById(R.id.InfoSign1).setVisibility(View.GONE);
        blurView = view.findViewById(R.id.blurView1);
        blurView.setVisibility(View.GONE);

        locationNameTextView = view.findViewById(R.id.locationNameTextView);
        locationNameTextView.setVisibility(View.GONE);
        regionNameTextView = view.findViewById(R.id.regionNameTextView);
        regionNameTextView.setVisibility(View.GONE);
        weatherIconImageView = view.findViewById(R.id.weatherIconImageView);
        weatherIconImageView.setVisibility(View.GONE);
        temperatureTextView = view.findViewById(R.id.temperatureTextView);
        windInfoTextView = view.findViewById(R.id.windInfoTextView);
        windDirTextView = view.findViewById(R.id.windDirTextView);
        weatherConditionView = view.findViewById(R.id.weatherConditionView);
        ImageView windDirImg = view.findViewById(R.id.windDirIcon);
        windDirImg.setVisibility(View.GONE);
        ProgressBar progressBar = requireActivity().findViewById(R.id.progressBar);
        ProgressBar progressBar2 = view.findViewById(R.id.progressBar2);
        ProgressBar progressBar3 = view.findViewById(R.id.progressBar3);
        progressBar.setVisibility(View.VISIBLE);
        progressBar2.setVisibility(View.VISIBLE);
        progressBar3.setVisibility(View.VISIBLE);
        weatherViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(WeatherViewModel.class);

        BlurView blurView = view.findViewById(R.id.blurView1);
        ViewGroup rootView = (ViewGroup) requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
        blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                //.setBlurAlgorithm(new RenderScriptBlur(requireContext(), true))
                .setBlurRadius(46f);
                //.setHasFixedTransformationMatrix(true);

        weatherViewModel.getWeatherDataList().observe(getViewLifecycleOwner(), avgData -> {
            if (avgData != null) {
                locationNameTextView.setText(locationName);
                ImageView backgroundImageWeather = requireActivity().findViewById(R.id.backgroundImageWeather);
                boolean isNight = avgData.isNight;
                int backgroundResId = WeatherRepository.getBackgroundResourceForCondition(avgData.weatherDescription, isNight);
                backgroundImageWeather.setImageResource(backgroundResId);
                Executor executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    String region = "Region not found";
                    try {
                        SharedPreferences prefs = requireContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
                        String cachedRegion = prefs.getString("region_" + locationName, null);
                        if (cachedRegion != null) {
                            region = cachedRegion;
                        } else {
                            region = WeatherRepository.getRegion(locationName);
                            prefs.edit().putString("region_" + locationName, region).apply();
                        }
                    } catch (Exception e) {
                        System.out.println("aaaa" + e);
                    }

                    String finalRegion = region;
                    handler.post(() -> {
                        regionNameTextView.setText(finalRegion);
                    });
                });

                windDirImg.setVisibility(View.VISIBLE);
                locationNameTextView.setVisibility(View.VISIBLE);
                regionNameTextView.setVisibility(View.VISIBLE);
                blurView.setVisibility(View.VISIBLE);
                temperatureTextView.setText(String.format("%.1f°C", avgData.temperature));
                windInfoTextView.setText(String.format("%.1f km/h", avgData.windSpeed));
                String compass = WeatherRepository.windDirectionToCompass(avgData.windDir);
                windDirTextView.setText(compass);
                weatherConditionView.setText(toTitleCase(avgData.weatherDescription));
                int iconResId = getIconResourceForCondition(avgData.weatherDescription, isNight);
                weatherIconImageView.setImageResource(iconResId);
                weatherIconImageView.setVisibility(View.VISIBLE);
                // progressBar.setVisibility(View.GONE);
            }
        });

        HorizontalScrollView scrollView = view.findViewById(R.id.dailyForecastScrollView);
        LinearLayout dailyForecastRow = view.findViewById(R.id.averageDailyForecastRow);
        LinearLayout hourlyForecastRow = view.findViewById(R.id.averageHourlyForecastRow);
        weatherViewModel.getDailyWeatherDataList().observe(getViewLifecycleOwner(), dailyList -> {
            if (dailyList != null) {
                dailyForecastRow.removeAllViews(); // clear any old boxes
                View firstDayBox = null;

                for (DailyWeatherData day : dailyList) {
                    View dayBox = inflater.inflate(R.layout.day_box, dailyForecastRow, false);

                    TextView dateTextView = dayBox.findViewById(R.id.dateTextView);
                    TextView relativeDay = dayBox.findViewById(R.id.relativeDateTextView);
                    ImageView iconImageView = dayBox.findViewById(R.id.weatherIconImageView2);
                    TextView maxTempTextView = dayBox.findViewById(R.id.maxTemp);
                    TextView minTempTextView = dayBox.findViewById(R.id.minTemp);
                    TextView conditionTextView = dayBox.findViewById(R.id.ConditionTextView);
                    TextView windTextView = dayBox.findViewById(R.id.windTextView);
                    TextView windDirTextView = dayBox.findViewById(R.id.windDirTextVieww);
                    BlurView blurView1 = dayBox.findViewById(R.id.blurView2);

                    LocalDate date = LocalDate.parse(day.date);
                    dateTextView.setText(formatDateDayOfWeek(date));
                    relativeDay.setText(getRelativeDayLabel(date));
                    iconImageView.setImageResource(getIconResourceForCondition(day.weatherDescription, false));
                    maxTempTextView.setText(String.format("%.0f°C", day.temperature));
                    minTempTextView.setText(String.format("%.0f°C", day.TempMin));
                    conditionTextView.setText(toTitleCase(day.weatherDescription));
                    windTextView.setText(String.format("%.0f km/h", day.windSpeed));
                    windDirTextView.setText(WeatherRepository.windDirectionToCompass(day.windDir));
                    // ViewGroup rootView = (ViewGroup) requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
                    // Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
                    blurView1.setupWith(rootView)
                            .setFrameClearDrawable(windowBackground)
                            .setBlurRadius(46f);

                    dailyForecastRow.addView(dayBox);
                    if (firstDayBox == null) {
                        firstDayBox = dayBox;
                    }

                    dayBox.setOnClickListener(v -> {
                        showHourlyPopup(day.hourlyData);
                    });
                }
                View finalFirstDayBox = firstDayBox; // must be effectively final for lambda
                ViewTreeObserver vto = scrollView.getViewTreeObserver();
                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (dailyForecastRow.getChildCount() == 0) return;
                        int boxWidth = dailyForecastRow.getChildAt(0).getWidth();
                        // Log.d("SCROLL", "Box width: " + boxWidth);
                        int todayIndex = getTodayIndex(dailyList);
                        // Log.d("SCROLL", "Today index: " + todayIndex + " boxWidth: " + boxWidth);
                        if (todayIndex >= 0 && todayIndex < dailyList.size()) {
                            scrollView.post(() -> {
                                int scrollX = Math.max(0, (todayIndex * boxWidth) - (boxWidth / 2));
                                scrollView.smoothScrollTo(scrollX, 0);
                            });
                        }
                    }
                });
                progressBar2.setVisibility(View.INVISIBLE);



                hourlyForecastRow.removeAllViews();
                View firstHourBox = null;
                for (HourlyWeatherData hour : dailyList.get(0).hourlyData) {
                    View hourBox = inflater.inflate(R.layout.hourly_forecast, hourlyForecastRow, false);

                    TextView hourTextView = hourBox.findViewById(R.id.hourText);
                    ImageView iconImageView = hourBox.findViewById(R.id.hourWeatherIcon);
                    TextView TempTextView = hourBox.findViewById(R.id.hourTempText);
                    // TextView conditionTextView = hourBox.findViewById(R.id.ConditionTextView);
                    TextView windTextView = hourBox.findViewById(R.id.hourWindText);
                    TextView windDirTextView = hourBox.findViewById(R.id.hourWindDirText);
                    BlurView blurView1 = hourBox.findViewById(R.id.blurView3);

                    LocalDateTime dateTime = LocalDateTime.parse(hour.dateTime);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    hourTextView.setText(dateTime.format(formatter));
                    iconImageView.setImageResource(getIconResourceForCondition(hour.weatherDescription, false));  // aaa
                    TempTextView.setText(String.format("%.0f°C", hour.temperature));
                    // conditionTextView.setText(toTitleCase(day.weatherDescription));
                    windTextView.setText(String.format("%.0f km/h", hour.windSpeed));
                    windDirTextView.setText(WeatherRepository.windDirectionToCompass(hour.windDir));
                    // ViewGroup rootView = (ViewGroup) requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
                    // Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
                    blurView1.setupWith(rootView)
                            .setFrameClearDrawable(windowBackground)
                            .setBlurRadius(46f);

                    hourlyForecastRow.addView(hourBox);
                    if (firstHourBox == null) {
                        firstHourBox = hourBox;
                    }
                }
                HorizontalScrollView scrollView2 = view.findViewById(R.id.HourlyForecastScrollView);
                View finalFirstHourBox = firstHourBox;
                ViewTreeObserver hourlyVto = hourlyForecastRow.getViewTreeObserver();
                hourlyVto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (hourlyForecastRow.getChildCount() == 0) return;
                        int boxWidth = hourlyForecastRow.getChildAt(0).getWidth();

                        // Get current hour
                        int currentHour = LocalTime.now().getHour();
                        int scrollToHourIndex = Math.max(0, currentHour);

                        if (scrollToHourIndex >= 0 && scrollToHourIndex < hourlyForecastRow.getChildCount()) {
                            scrollView2.post(() -> {
                                int scrollX = Math.max(0, (scrollToHourIndex * boxWidth) - (boxWidth / 2));
                                scrollView2.smoothScrollTo(scrollX, 0);
                            });
                        }
                        // Remove listener after first trigger to avoid repeat calls
                        hourlyForecastRow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
                progressBar3.setVisibility(View.INVISIBLE);
                /*SharedPreferences prefs = requireContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                Gson gson = new Gson();
                String json = gson.toJson(dailyList);
                editor.putString("daily_cache_" + locationName, json);
                editor.apply();
                 */
            }
        });
        // progressBar2.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        //weatherViewModel.loadWeather(locationName);
        return view;
    }

    private void showHourlyPopup(List<HourlyWeatherData> hourlyList) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.hourly_bottom_sheet, null);
        LinearLayout hourlyRow = sheetView.findViewById(R.id.hourlyForecastRow);

        ViewGroup rootView = (ViewGroup) requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        for (HourlyWeatherData hour : hourlyList) {
            View hourBox = getLayoutInflater().inflate(R.layout.hourly_forecast, hourlyRow, false);

            View constraint1 = hourBox.findViewById(R.id.constraintView1);
            constraint1.setBackgroundResource(R.drawable.second_forecast_hour_box);
            // constraint1.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
            TextView hourTextView = hourBox.findViewById(R.id.hourText);
            ImageView iconImageView = hourBox.findViewById(R.id.hourWeatherIcon);
            TextView TempTextView = hourBox.findViewById(R.id.hourTempText);
            TextView windTextView = hourBox.findViewById(R.id.hourWindText);
            TextView windDirTextView = hourBox.findViewById(R.id.hourWindDirText);
            BlurView blurView = hourBox.findViewById(R.id.blurView3);
            float radius = 2f;
            float dx = 0f;
            float dy = 0f;
            int shadowColor = Color.parseColor("#60000000");
            // int textColor = Color.parseColor("#c5ccd6");
            int textColor = Color.GRAY;

            LocalDateTime dateTime = safeParseDateTime(hour.dateTime);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            hourTextView.setText(dateTime.format(formatter));
            iconImageView.setImageResource(getIconResourceForCondition(hour.weatherDescription, false));
            TempTextView.setText(String.format("%.0f°C", hour.temperature));
            windTextView.setText(String.format("%.0f km/h", hour.windSpeed));
            windDirTextView.setText(WeatherRepository.windDirectionToCompass(hour.windDir));
            hourTextView.setTextColor(textColor);
            //hourTextView.setShadowLayer(radius, dx, dy, shadowColor);
            TempTextView.setTextColor(textColor);
            //TempTextView.setShadowLayer(radius, dx, dy, shadowColor);
            windTextView.setTextColor(textColor);
            //windTextView.setShadowLayer(radius, dx, dy, shadowColor);
            windDirTextView.setTextColor(textColor);
            //windDirTextView.setShadowLayer(radius, dx, dy, shadowColor);

            blurView.setupWith(rootView)
                    .setFrameClearDrawable(rootView.getBackground())
                    .setBlurRadius(46f);

            hourlyRow.addView(hourBox);
        }

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }


    private static LocalDateTime safeParseDateTime(String input) {
        if (input.contains("T24:00:00")) {
            String datePart = input.substring(0, 10);
            LocalDate date = LocalDate.parse(datePart).plusDays(1);  // shift to next day
            return LocalDateTime.of(date, LocalTime.MIDNIGHT);       // 00:00:00 of next day
        } else {
            return LocalDateTime.parse(input);
        }
    }


    private int getTodayIndex(List<DailyWeatherData> dailyList) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < dailyList.size(); i++) {
            if (LocalDate.parse(dailyList.get(i).date).equals(today)) {
                return i;
            }
        }
        return 0; // fallback if today not found
    }

    private String formatDateDayOfWeek(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E d", Locale.getDefault());
        return date.format(formatter);
    }

    private String getRelativeDayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "Today";
        if (date.equals(today.minusDays(1))) return "Yesterday";
        if (date.equals(today.plusDays(1))) return "Tomorrow";
        return " ";
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;

        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }

        return sb.toString().trim();
    }

    private int getIconResourceForCondition(String condition, boolean isNight) {
        condition = condition.toLowerCase();
        // boolean isNight = condition.contains("night");
        if (condition.contains("sunny") && isNight) return R.drawable.ic_night;
        if (condition.contains("sunny")) return R.drawable.ic_day;
        if (condition.contains("partly cloudy") && isNight) return R.drawable.ic_partly_cloudy_night;
        if (condition.contains("partly cloudy")) return R.drawable.ic_partly_cloudy;
        if (condition.contains("cloudy") && isNight) return R.drawable.ic_partly_cloudy_night;
        if (condition.contains("cloudy")) return R.drawable.ic_cloudy;
        if (condition.contains("rain") && isNight) return R.drawable.ic_rain_night;
        if (condition.contains("rain")) return R.drawable.ic_rain;
        if (condition.contains("mist")) return R.drawable.ic_fog;
        if (condition.contains("fog")) return R.drawable.ic_fog;
        if (condition.contains("precipitation")) return R.drawable.ic_rain;
        if (condition.contains("dust") || condition.contains("sand")) return R.drawable.ic_fog;
        if (condition.contains("drizzle")) return R.drawable.ic_rain;
        if (condition.contains("freezing drizzle")) return R.drawable.ic_shower;
        if (condition.contains("rain and snow")) return R.drawable.ic_snow2;
        if (condition.contains("freezing rain")) return R.drawable.ic_snow2;
        if (condition.contains("snow")) return R.drawable.ic_snow;
        if (condition.contains("showers")) return R.drawable.ic_shower;
        if (condition.contains("thunderstorm with hail")) return R.drawable.ic_thunder;
        if (condition.contains("thunderstorm") || condition.contains("thunder")) return R.drawable.ic_thunder2;
        return R.drawable.ic_unknown;
    }
}