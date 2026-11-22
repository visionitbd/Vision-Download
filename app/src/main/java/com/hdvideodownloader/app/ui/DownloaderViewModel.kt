package com.hdvideodownloader.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdvideodownloader.app.DownloaderApplication
import com.hdvideodownloader.app.data.VideoDownloadRepository
import com.hdvideodownloader.app.model.DownloadStatus
import com.hdvideodownloader.app.model.DownloadedItem
import com.hdvideodownloader.app.model.QualityOption
import com.hdvideodownloader.app.model.SocialPlatform
import com.hdvideodownloader.app.model.VideoMetadata
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DownloaderUiState(
    val url: String = "",
    val detectedPlatform: SocialPlatform? = null,
    val selectedQuality: QualityOption = QualityOption.BEST_HD,
    val metadata: VideoMetadata? = null,
    val status: DownloadStatus = DownloadStatus.Idle,
    val isUpdatingEngine: Boolean = false,
    val engineMessage: String? = null,
    val downloads: List<DownloadedItem> = emptyList()
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoDownloadRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(DownloaderUiState())
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    val engineReady: StateFlow<Boolean> = DownloaderApplication.engineReady
    val initError: StateFlow<String?> = DownloaderApplication.initError

    private var activeProcessId: String? = null
    private var downloadJob: Job? = null

    init {
        refreshDownloads()
    }

    fun onUrlChanged(newUrl: String) {
        val trimmed = newUrl.trim()
        val platform = if (trimmed.startsWith("http")) SocialPlatform.detect(trimmed) else null
        _uiState.update {
            it.copy(
                url = newUrl,
                detectedPlatform = platform,
                metadata = if (trimmed != it.url.trim()) null else it.metadata
            )
        }
    }

    fun onQualitySelected(quality: QualityOption) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun fetchPreview() {
        val targetUrl = _uiState.value.url.trim()
        if (!targetUrl.startsWith("http")) {
            _uiState.update {
                it.copy(status = DownloadStatus.Error("সঠিক YouTube, Facebook বা TikTok ভিডিও লিংক দিন।"))
            }
            return
        }
        if (!engineReady.value) {
            _uiState.update {
                it.copy(status = DownloadStatus.Error("ডাউনলোডার ইঞ্জিন প্রস্তুত হচ্ছে, একটু অপেক্ষা করুন..."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = DownloadStatus.FetchingInfo) }
            runCatching { repository.fetchVideoMetadata(targetUrl) }
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(metadata = info, status = DownloadStatus.Idle)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            status = DownloadStatus.Error(
                                "ভিডিও তথ্য আনা যায়নি: ${err.localizedMessage ?: "অজানা ত্রুটি"}"
                            )
                        )
                    }
                }
        }
    }

    fun startDownload() {
        val targetUrl = _uiState.value.url.trim()
        if (!targetUrl.startsWith("http")) {
            _uiState.update {
                it.copy(status = DownloadStatus.Error("অনুগ্রহ করে একটি বৈধ ভিডিও লিংক পেস্ট করুন।"))
            }
            return
        }
        if (!engineReady.value) {
            _uiState.update {
                it.copy(status = DownloadStatus.Error("ইঞ্জিন এখনো চালু হয়নি, কয়েক সেকেন্ড পর আবার চেষ্টা করুন।"))
            }
            return
        }

        val processId = "dl_${UUID.randomUUID()}"
        activeProcessId = processId

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(status = DownloadStatus.Downloading(0f, 0L, "ডাউনলোড শুরু হচ্ছে..."))
            }

            runCatching {
                repository.downloadVideo(
                    url = targetUrl,
                    quality = _uiState.value.selectedQuality,
                    processId = processId
                ) { progress, eta, line ->
                    _uiState.update {
                        it.copy(
                            status = DownloadStatus.Downloading(
                                progress = progress.coerceIn(0f, 100f),
                                etaSeconds = eta,
                                logLine = line
                            )
                        )
                    }
                }
            }.onSuccess { files ->
                activeProcessId = null
                refreshDownloads()
                val msg = if (files.isNotEmpty()) {
                    "সফলভাবে সেভ হয়েছে: ${files.first().name}"
                } else {
                    "ডাউনলোড সম্পন্ন হয়েছে! Downloads/VisionDownload ফোল্ডার দেখুন।"
                }
                _uiState.update { it.copy(status = DownloadStatus.Success(msg)) }
            }.onFailure { err ->
                activeProcessId = null
                _uiState.update {
                    it.copy(
                        status = DownloadStatus.Error(
                            "ডাউনলোড ব্যর্থ হয়েছে: ${err.localizedMessage ?: "লিংকটি প্রাইভেট বা অবৈধ হতে পারে"}"
                        )
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        activeProcessId?.let { repository.cancelDownload(it) }
        downloadJob?.cancel()
        activeProcessId = null
        _uiState.update { it.copy(status = DownloadStatus.Idle) }
    }

    fun updateEngine() {
        if (!engineReady.value || _uiState.value.isUpdatingEngine) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingEngine = true, engineMessage = "yt-dlp আপডেট চেক করা হচ্ছে...") }
            runCatching { repository.updateEngine() }
                .onSuccess { status ->
                    val message = when (status) {
                        YoutubeDL.UpdateStatus.DONE -> "ইঞ্জিন সফলভাবে আপডেট হয়েছে!"
                        YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "ইঞ্জিন আগে থেকেই সর্বশেষ ভার্সনে আছে।"
                        else -> "আপডেট সম্পন্ন হয়েছে।"
                    }
                    _uiState.update { it.copy(isUpdatingEngine = false, engineMessage = message) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isUpdatingEngine = false,
                            engineMessage = "আপডেট করা যায়নি: ${err.localizedMessage ?: "ইন্টারনেট চেক করুন"}"
                        )
                    }
                }
        }
    }

    fun refreshDownloads() {
        viewModelScope.launch {
            val items = repository.listDownloadedFiles()
            _uiState.update { it.copy(downloads = items) }
        }
    }
}
