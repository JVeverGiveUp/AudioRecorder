package com.ptdstudio.internalsoundrecorder.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by Hero on 3/6/2017.
 */

public class PrivacyPolicy {
    public static void start(Context context){
        Uri uri = Uri.parse("https://www.freeprivacypolicy.com/live/349240cd-757f-4e6a-b1ad-765aca7f4399");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {

        }
    }
}
