package com.allonsy.laserdistancer;


import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF_DISTANCE = "distance";
    private static final String PREF_IMAGE_PATH = "imagePath";


    public static float getDistance(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getFloat(PREF_DISTANCE, 0);
    }

    public static void setDistance(Context context, float distance) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putFloat(PREF_DISTANCE, distance)
                .apply();
    }

    public static String getImagePath(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_IMAGE_PATH, "");
    }

    public static void setImagePath(Context context, String imagePath) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_IMAGE_PATH, imagePath)
                .apply();
    }
}