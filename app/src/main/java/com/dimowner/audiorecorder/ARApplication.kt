/*
 * Copyright 2018 Dmytro Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")

package com.dimowner.audiorecorder

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.dimowner.audiorecorder.util.AndroidUtils
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus
import com.ptdstudio.internalsoundrecorder.appopenad.AppOpenHelper
import com.ptdstudio.internalsoundrecorder.appopenad.OnShowAdCompleteListener
import com.ptdstudio.internalsoundrecorder.util.ProVersionManager
import timber.log.Timber
import timber.log.Timber.DebugTree


//import com.google.firebase.FirebaseApp;
open class ARApplication : Application() {

    private var audioOutputChangeReceiver: AudioOutputChangeReceiver? = null
    private var rebootReceiver: RebootReceiver? = null

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            //Timber initialization
            Timber.plant(DebugTree())
        }
        super.onCreate()
        INSTANCE = this
        PACKAGE_NAME = applicationContext.packageName
        applicationHandler = Handler(applicationContext.mainLooper)
        screenWidthDp = AndroidUtils.pxToDp(
            AndroidUtils.getScreenWidth(
                applicationContext
            )
        )
        val prefs = injector.providePrefs(applicationContext)
        if (!prefs.isMigratedSettings) {
            prefs.migrateSettings()
        }
        registerAudioOutputChangeReceiver()
        registerRebootReceiver()

        // feature: pause when phone functions ringing or off-hook
        try {
            val telephonyMgr = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT<Build.VERSION_CODES.S) {
                telephonyPreAndroid12(telephonyMgr)
            } else {
                telephonyAndroid12Plus(telephonyMgr)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        //		FirebaseApp.initializeApp(this);
        //		FirebaseApp.initializeApp(this);
        proVersionManager = ProVersionManager.getINSTANCE(this)
        if (!proVersionManager!!.isNoAdsVersion) {
            MobileAds.initialize(this)
            { initializationStatus: InitializationStatus? -> }
            setupConfigAds()
            appOpenHelper = AppOpenHelper(this)
        }

    }

    override fun onTerminate() {
        super.onTerminate()
        //This method is never called on real Android devices
        injector.releaseMainPresenter()
        injector.closeTasks()
        unregisterReceiver(audioOutputChangeReceiver)
        unregisterReceiver(rebootReceiver)
    }

    fun pausePlayback() {
        //Pause playback when incoming call or on hold
        val player = injector.provideAudioPlayer()
        player.pause()
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    fun telephonyAndroid12Plus(telephonyMgr: TelephonyManager) {
        if (ContextCompat.checkSelfPermission(applicationContext,
            Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyMgr.registerTelephonyCallback(mainExecutor, callStateListener!!)
        }
    }

    private fun registerAudioOutputChangeReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        audioOutputChangeReceiver = AudioOutputChangeReceiver()
        registerReceiver(audioOutputChangeReceiver, intentFilter)
    }

    private fun registerRebootReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_REBOOT)
        intentFilter.addAction(Intent.ACTION_SHUTDOWN)
        rebootReceiver = RebootReceiver()
        registerReceiver(rebootReceiver, intentFilter)
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private abstract class CallStateListener : TelephonyCallback(),
        TelephonyCallback.CallStateListener {
        abstract override fun onCallStateChanged(state: Int)
    }

    private val callStateListener: CallStateListener? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) object : CallStateListener() {
            override fun onCallStateChanged(state: Int) {
                if (state == TelephonyManager.CALL_STATE_OFFHOOK ||
                        state == TelephonyManager.CALL_STATE_RINGING) {
                    pausePlayback()
                }
            }
        } else null

    private class AudioOutputChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val actionOfIntent = intent.action
            if (actionOfIntent != null && actionOfIntent == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val player = injector.provideAudioPlayer()
                player.pause()
            }
        }
    }

    private class RebootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_REBOOT || intent?.action == Intent.ACTION_SHUTDOWN) {
                val appRecorder = injector.provideAppRecorder(context)
                val audioPlayer = injector.provideAudioPlayer()
                appRecorder.stopRecording()
                audioPlayer.stop()
            }
        }
    }

    @Suppress("DEPRECATION")
    fun telephonyPreAndroid12(telephonyMgr: TelephonyManager) {
        telephonyMgr.listen(mPhoneStateListenerPreAndroid12, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private val mPhoneStateListenerPreAndroid12: PhoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                pausePlayback()
            }
            super.onCallStateChanged(state, incomingNumber)
        }
    }

    companion object {
        private var PACKAGE_NAME: String? = null

        @JvmField
		@Volatile
        var applicationHandler: Handler? = null

        /** Screen width in dp  */
        private var screenWidthDp = 0f
        @JvmStatic
		var injector = Injector()
        @JvmStatic
		fun appPackage(): String? {
            return PACKAGE_NAME
        }

        /**
         * Calculate density pixels per second for record duration.
         * Used for visualisation waveform in view.
         * @param durationSec record duration in seconds
         */
		@JvmStatic
		fun getDpPerSecond(durationSec: Float): Float {
            return if (durationSec > AppConstants.LONG_RECORD_THRESHOLD_SECONDS) {
                AppConstants.WAVEFORM_WIDTH * screenWidthDp / durationSec
            } else {
                AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat()
            }
        }

        @JvmStatic
		val longWaveformSampleCount: Int
            get() = (AppConstants.WAVEFORM_WIDTH * screenWidthDp).toInt()


        private var INSTANCE: ARApplication? = null
        fun getInstance(): ARApplication {
            return INSTANCE!!
        }
    }

    var proVersionManager: ProVersionManager? = null

    private var appOpenHelper: AppOpenHelper? = null

    private fun setupConfigAds() {
        val testDevices: MutableList<String> = ArrayList()
        testDevices.add("28C19B95FA33B8ADF62C2A3409395068")
        testDevices.add("85B7D76CD7221E9F1ADABEEFEE2D0B8A")
        testDevices.add("E064B31E4691F56D63FD841E017E27CD")

        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDevices)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
    }

    fun showAdIfAvailable(
        activity: Activity,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenHelper?.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    fun isShowingAds(): Boolean {
        //in case premium version, appOpenHelper will be null
        if (appOpenHelper != null) return appOpenHelper!!.isGrantedPermissionShowAds
        return false
    }



}