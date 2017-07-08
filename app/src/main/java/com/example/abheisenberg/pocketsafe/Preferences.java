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
