package com.ptdstudio.internalsoundrecorder.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.dimowner.audiorecorder.data.PrefsImpl;

public class ProVersionManager {
    Context context;
    private static ProVersionManager INSTANCE;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private static final String KEY_NO_ADS_VERSION = "key_audio_recorder_pro";
    private static final String KEY_ALL_FEATURES = "key_audio_all_features";

    private ProVersionManager(Context context){
        this.context = context;
        INSTANCE = this;
        preferences = context.getSharedPreferences(PrefsImpl.PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }
    public static ProVersionManager getINSTANCE(Context context){
        if(INSTANCE == null)
            INSTANCE = new ProVersionManager(context);
        return INSTANCE;
    }

    public void setNoAds(boolean value){
        editor.putBoolean(KEY_NO_ADS_VERSION, value);
        editor.commit();
    }
    public void setAllFeatures(boolean value){
        editor.putBoolean(KEY_ALL_FEATURES, value);
        editor.commit();
    }

    public boolean isNoAdsVersion(){
        return preferences.getBoolean(KEY_NO_ADS_VERSION, false);
    }
    public boolean isAllFeatures(){
        return preferences.getBoolean(KEY_ALL_FEATURES, false);
//        return true;
    }
}
