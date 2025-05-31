package com.example.aclass.utils;

import android.content.Context;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static final String CLOUD_NAME = "dxpojpgks";
    private static final String API_KEY = "342767545628923";
    private static final String API_SECRET = "QRmBZWoR5z_VwVrzAScJ8KKK06Q";
    private static final String UPLOAD_PRESET = "android_uploads";

    // Track initialization status manually
    private static boolean isInitialized = false;

    public static void initCloudinary(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("api_key", API_KEY);
            config.put("api_secret", API_SECRET);

            MediaManager.init(context.getApplicationContext(), config);
            isInitialized = true;
        }
    }

    public static String getUploadPreset() {
        return UPLOAD_PRESET;
    }
}
