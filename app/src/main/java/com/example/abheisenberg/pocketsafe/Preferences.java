package com.example.abheisenberg.pocketsafe;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by abheisenberg on 8/7/17.
 */

public class Preferences {

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    /*
        A getter and setter for every setting list item.
     */

    public static SharedPreferences getSharedPref(Context context){
        return context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
    }

    public static boolean getIfFlashOn(Context context){
        return getSharedPref(context).getBoolean("flashlight",true);
    }

    public static void setIfFlashOn(Context context, boolean isSet){
        SharedPreferences.Editor editor;
        editor = getSharedPref(context).edit();
        editor.putBoolean("flashlight",isSet);
        editor.apply();
    }

    public static int getGraceTime(Context context){
        return getSharedPref(context).getInt("grace_countdown_time",5);
    }

    public static void setGraceTime(Context context, int isSet){
        SharedPreferences.Editor editor;
        editor = getSharedPref(context).edit();
        editor.putInt("grace_countdown_time",isSet);
        editor.apply();
    }

    public static int getTimeToShowOnSpinner(Context context){
        return getSharedPref(context).getInt("to_show_on_spinner",0);
    }

    public static void setTimeToShowOnSpinner(Context context, int isSet){
        SharedPreferences.Editor editor;
        editor = getSharedPref(context).edit();
        editor.putInt("to_show_on_spinner",isSet);
        editor.apply();
    }

    public static boolean getCountdownSound(Context context){
        return getSharedPref(context).getBoolean("countdownSound",true);
    }

    public static void setCountdownSound(Context context, boolean isSet){
        SharedPreferences.Editor editor;
        editor = getSharedPref(context).edit();
        editor.putBoolean("countdownSound",isSet);
        editor.apply();
    }

    public static boolean getIfExtraPw(Context context){
        return getSharedPref(context).getBoolean("ifExtraPw",false);
    }

    public static void setIfExtraPw(Context context, boolean isSet){
        SharedPreferences.Editor editor;
        editor = getSharedPref(context).edit();
        editor.putBoolean("ifExtraPw",isSet);
        editor.apply();
    }

    public static String getExtraPw(Context context){
        return getSharedPref(context).getString("extraPw","");
    }

    public static void setExtraPw(Context context, String isSet){
        SharedPreferences.Editor editor;
        editor = getSharedPref(context).edit();
        editor.putString("extraPw",isSet);
        editor.apply();
    }

}
