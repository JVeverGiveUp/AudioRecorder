package com.ptdstudio.internalsoundrecorder.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dimowner.audiorecorder.data.PrefsImpl;
import com.ptdstudio.internalsoundrecorder.InAppPurchased;

public class ProVersionManager {
    Context context;
    private static ProVersionManager INSTANCE;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private static final String KEY_NO_ADS_VERSION = "key_audio_recorder_pro";
    private static final String KEY_ALL_FEATURES = "key_audio_all_features";

    private static final String KEY_NOADS_PRICE = "key_noads_price";
    private static final String KEY_ALL_FEATURES_PRICE = "key_all_features_price";
    private static final String KEY_PREMIUM_PRICE = "key_premium_price";

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

    public void setPrice(String key, String price){
        if(InAppPurchased.ITEM_PREMIUM.equals(key))
            editor.putString(KEY_PREMIUM_PRICE, price);
        else if(InAppPurchased.ITEM_ALL_FEATURES.equals(key))
            editor.putString(KEY_ALL_FEATURES_PRICE, price);
        else if(InAppPurchased.ITEM_NO_ADS.equals(key))
            editor.putString(KEY_NOADS_PRICE, price);
        editor.apply();
//        Log.d("ProVersionManager", "setPrice: " + key + " = " + price);
    }

    public String getPrice(String key){
        String defaultPrice = "BUY";
        if(InAppPurchased.ITEM_PREMIUM.equals(key))
            return preferences.getString(KEY_PREMIUM_PRICE, defaultPrice);
        else if(InAppPurchased.ITEM_ALL_FEATURES.equals(key))
            return preferences.getString(KEY_ALL_FEATURES_PRICE, defaultPrice);
        else if(InAppPurchased.ITEM_NO_ADS.equals(key))
            return preferences.getString(KEY_NOADS_PRICE, defaultPrice);
        return defaultPrice;
    }
}
