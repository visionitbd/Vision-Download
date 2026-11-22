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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
    val metadataQuality: QualityOption? = null,
    val status: DownloadStatus = DownloadStatus.Idle,
    val isUpdatingEngine: Boolean = false,
    val engineMessage: String? = null,
    val downloads: List<DownloadedItem> = emptyList(),
    val playingPreviewUrl: String? = null,
    val playingPreviewTitle: String? = null
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoDownloadRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(DownloaderUiState())
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    val engineReady: StateFlow<Boolean> = DownloaderApplication.engineReady
    val initError: StateFlow<String?> = DownloaderApplication.initError

    private var activeProcessId: String? = null
    private var downloadJob: Job? = null
    private var prefetchJob: Deferred<VideoMetadata?>? = null
    private var prefetchUrl: String? = null
    private var prefetchQuality: QualityOption? = null

    private val percentRegex = Regex("(\\d+(?:\\.\\d+)?)%")
    private val speedRegex = Regex("at\\s+([0-9.]+[A-Za-z]+/s)")

    init {
        refreshDownloads()
    }

    fun onUrlChanged(newUrl: String) {
        val trimmed = newUrl.trim()
        val platform = if (trimmed.startsWith("http")) SocialPlatform.detect(trimmed) else null
        val changed = trimmed != _uiState.value.url.trim()
        _uiState.update {
            it.copy(
                url = newUrl,
                detectedPlatform = platform,
                metadata = if (changed) null else it.metadata,
                metadataQuality = if (changed) null else it.metadataQuality
            )
        }
        if (changed && trimmed.startsWith("http")) {
            scheduleBackgroundPrefetch(trimmed, _uiState.value.selectedQuality)
        } else if (trimmed.isEmpty()) {
            prefetchJob?.cancel()
            prefetchJob = null
        }
    }

    fun onQualitySelected(quality: QualityOption) {
        val prev = _uiState.value.selectedQuality
        _uiState.update { it.copy(selectedQuality = quality) }
        val url = _uiState.value.url.trim()
        if (prev != quality && url.startsWith("http")) {
            scheduleBackgroundPrefetch(url, quality)
        }
    }

    /**
     * Quietly pre-fetches video metadata & direct stream URL right after pasting a link,
     * so tapping "Download" starts streaming almost instantaneously.
     */
    private fun scheduleBackgroundPrefetch(url: String, quality: QualityOption) {
        if (!engineReady.value) return
        prefetchJob?.cancel()
        prefetchUrl = url
        prefetchQuality = quality
        prefetchJob = viewModelScope.async {
            delay(350)
            val result = runCatching { repository.fetchVideoMetadata(url, quality) }.getOrNull()
            if (result != null && _uiState.value.url.trim() == url) {
                _uiState.update {
                    it.copy(metadata = result, metadataQuality = quality)
                }
            }
            result
        }
    }

    fun openPreviewPlayer() {
        val meta = _uiState.value.metadata ?: return
        val streamUrl = meta.streamUrl
        if (!streamUrl.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    playingPreviewUrl = streamUrl,
                    playingPreviewTitle = meta.title
                )
            }
        } else {
            _uiState.update {
                it.copy(status = DownloadStatus.Error("এই ভিডিওর সরাসরি প্রিভিউ প্লে সাপোর্ট করছে না, সরাসরি ডাউনলোড করুন।"))
            }
        }
    }

    fun closePreviewPlayer() {
        _uiState.update { it.copy(playingPreviewUrl = null, playingPreviewTitle = null) }
    }

    fun fetchPreview(autoPlay: Boolean = false) {
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

        val quality = _uiState.value.selectedQuality
        viewModelScope.launch {
            _uiState.update { it.copy(status = DownloadStatus.FetchingInfo) }
            val prefetched = if (prefetchUrl == targetUrl && prefetchQuality == quality) {
                runCatching { prefetchJob?.await() }.getOrNull()
            } else null

            val result = if (prefetched != null) {
                Result.success(prefetched)
            } else {
                runCatching { repository.fetchVideoMetadata(targetUrl, quality) }
            }

            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        metadata = info,
                        metadataQuality = quality,
                        status = DownloadStatus.Idle,
                        playingPreviewUrl = if (autoPlay && !info.streamUrl.isNullOrBlank()) info.streamUrl else it.playingPreviewUrl,
                        playingPreviewTitle = if (autoPlay && !info.streamUrl.isNullOrBlank()) info.title else it.playingPreviewTitle
                    )
                }
            }.onFailure { err ->
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

        val selectedQuality = _uiState.value.selectedQuality
        val processId = "dl_${UUID.randomUUID()}"
        activeProcessId = processId

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = DownloadStatus.Downloading(
                        progress = 0f,
                        etaSeconds = 0L,
                        speedText = null,
                        statusLabel = "লিংক প্রস্তুত হচ্ছে..."
                    )
                )
            }

            // 1. Reuse already-cached metadata OR await the in-flight background pre-fetch
            var readyMeta = _uiState.value.metadata?.takeIf {
                _uiState.value.metadataQuality == selectedQuality && !it.streamUrl.isNullOrBlank()
            }
            if (readyMeta == null && prefetchUrl == targetUrl && prefetchQuality == selectedQuality) {
                readyMeta = runCatching { prefetchJob?.await() }.getOrNull()
                    ?.takeIf { !it.streamUrl.isNullOrBlank() }
            }

            // 2. Fast Path: High-speed native Android HTTP stream (256 KB buffer)
            if (readyMeta != null && !readyMeta.streamUrl.isNullOrBlank()) {
                val directResult = runCatching {
                    repository.downloadDirectStream(
                        streamUrl = readyMeta.streamUrl,
                        title = readyMeta.title,
                        ext = readyMeta.ext
                    ) { progress, eta, speed ->
                        _uiState.update { state ->
                            state.copy(
                                status = DownloadStatus.Downloading(
                                    progress = progress,
                                    etaSeconds = eta,
                                    speedText = speed,
                                    statusLabel = "ডাউনলোড হচ্ছে"
                                )
                            )
                        }
                    }
                }

                if (directResult.isSuccess) {
                    activeProcessId = null
                    finishDownloadSuccess(directResult.getOrNull()?.name)
                    return@launch
                } else if (directResult.exceptionOrNull() is CancellationException) {
                    activeProcessId = null
                    return@launch
                }
                // Direct stream failed (e.g. token/header required); fall back seamlessly to yt-dlp
            }

            // 3. Reliable Fallback: yt-dlp with 8 concurrent fragments
            var highestProgress = 0f
            runCatching {
                repository.downloadVideo(
                    url = targetUrl,
                    quality = selectedQuality,
                    processId = processId
                ) { progress, eta, line ->
                    val parsedPct = when {
                        progress in 0f..100f -> progress
                        else -> percentRegex.find(line)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: -1f
                    }
                    if (parsedPct > highestProgress) {
                        highestProgress = parsedPct.coerceIn(0f, 100f)
                    }
                    val speed = speedRegex.find(line)?.groupValues?.getOrNull(1)
                    _uiState.update { state ->
                        val prevSpeed = (state.status as? DownloadStatus.Downloading)?.speedText
                        state.copy(
                            status = DownloadStatus.Downloading(
                                progress = highestProgress,
                                etaSeconds = if (eta > 0) eta else 0L,
                                speedText = speed ?: prevSpeed,
                                statusLabel = if (highestProgress > 0f) "ডাউনলোড হচ্ছে" else "ডাউনলোড শুরু হচ্ছে..."
                            )
                        )
                    }
                }
            }.onSuccess { files ->
                activeProcessId = null
                finishDownloadSuccess(files.firstOrNull()?.name)
            }.onFailure { err ->
                activeProcessId = null
                if (err !is CancellationException) {
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
    }

    private suspend fun finishDownloadSuccess(fileName: String?) {
        _uiState.update {
            it.copy(
                status = DownloadStatus.Downloading(
                    progress = 100f,
                    etaSeconds = 0L,
                    speedText = null,
                    statusLabel = "ডাউনলোড সম্পন্ন"
                )
            )
        }
        refreshDownloads()
        delay(500)
        val msg = if (!fileName.isNullOrBlank()) {
            "১০০% সম্পন্ন! সেভ হয়েছে: $fileName"
        } else {
            "১০০% ডাউনলোড সম্পন্ন! Downloads/VisionDownload ফোল্ডার দেখুন।"
        }
        _uiState.update { it.copy(status = DownloadStatus.Success(msg, 100f)) }
    }

    fun cancelDownload() {
        activeProcessId?.let { repository.cancelDownload(it) }
        downloadJob?.cancel()
        activeProcessId = null
        _uiState.update { it.copy(status = DownloadStatus.Idle) }
    }

    fun deleteDownload(item: DownloadedItem) {
        viewModelScope.launch {
            repository.deleteFile(item.file)
            refreshDownloads()
        }
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
