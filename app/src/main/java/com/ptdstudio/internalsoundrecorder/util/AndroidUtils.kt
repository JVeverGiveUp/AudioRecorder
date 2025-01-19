package com.ptdstudio.internalsoundrecorder.util

import android.app.AlertDialog
import android.view.View
import android.widget.Button
import com.dimowner.audiorecorder.R
import com.dimowner.audiorecorder.app.settings.SettingsActivity
import com.ptdstudio.internalsoundrecorder.InAppPurchased


object AndroidUtils {
    fun showPremiumDialog(activity: SettingsActivity, inAppPurchased: InAppPurchased) {
        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setCancelable(true)
        val rootView: View =
            activity.layoutInflater.inflate(R.layout.inapppurchased_layout, null, false)
        val btnPremium: Button = rootView.findViewById(R.id.btn_premium)
        val btnNoAds: Button = rootView.findViewById(R.id.btn_no_ads)
        val btnAllFeatures: Button = rootView.findViewById(R.id.btn_all_features)
        val btnOk: Button = rootView.findViewById(R.id.dialog_positive_btn)
        btnPremium.setOnClickListener { view: View? ->
            inAppPurchased.purchased(
                activity,
                InAppPurchased.ITEM_PREMIUM
            )
        }
        btnNoAds.setOnClickListener { view: View? ->
            inAppPurchased.purchased(
                activity,
                InAppPurchased.ITEM_NO_ADS
            )
        }
        btnAllFeatures.setOnClickListener { view: View? ->
            inAppPurchased.purchased(
                activity,
                InAppPurchased.ITEM_ALL_FEATURES
            )
        }
        dialogBuilder.setView(rootView)
        val alertDialog = dialogBuilder.create()
        btnOk.setOnClickListener { v: View? -> alertDialog.dismiss() }
        alertDialog.show()
    }
}