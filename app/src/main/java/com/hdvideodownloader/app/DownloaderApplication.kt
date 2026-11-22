package com.hdvideodownloader.app

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloaderApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "DownloaderApp"
        private val _engineReady = MutableStateFlow(false)
        val engineReady: StateFlow<Boolean> = _engineReady.asStateFlow()

        private val _initError = MutableStateFlow<String?>(null)
        val initError: StateFlow<String?> = _initError.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@DownloaderApplication)
                _engineReady.value = true
                Log.i(TAG, "YoutubeDL initialized successfully")
            } catch (e: YoutubeDLException) {
                Log.e(TAG, "Failed to initialize youtubedl-android", e)
                _initError.value = e.localizedMessage ?: "ইঞ্জিন চালু করা যায়নি"
            }
        }
    }
}
