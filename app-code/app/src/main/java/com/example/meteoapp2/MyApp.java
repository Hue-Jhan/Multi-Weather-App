package com.example.meteoapp2;

import android.app.Application;
import android.content.Context;

public class MyApp extends Application {
    private static MyApp instance;

    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
