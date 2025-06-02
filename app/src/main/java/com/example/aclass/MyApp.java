package com.example.aclass;

import android.app.Application;
import com.example.aclass.utils.CloudinaryConfig;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Cloudinary once when app starts
        CloudinaryConfig.initCloudinary(this);
    }
}
