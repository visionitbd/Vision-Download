package com.hdvideodownloader.app.model

import java.io.File

enum class SocialPlatform(val displayName: String) {
    YOUTUBE("YouTube"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    OTHER("অন্যান্য লিংক");

    companion object {
        fun detect(url: String): SocialPlatform {
            val lower = url.lowercase()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("tiktok.com") -> TIKTOK
                lower.contains("facebook.com") ||
                    lower.contains("fb.watch") ||
                    lower.contains("fb.com") -> FACEBOOK
                else -> OTHER
            }
        }
    }
}

enum class QualityOption(
    val label: String,
    val subtitle: String,
    val formatSelector: String
) {
    BEST_HD(
        label = "সর্বোচ্চ HD (MP4)",
        subtitle = "সেরা কোয়ালিটি (ওয়াটারমার্ক ছাড়া)",
        formatSelector = "best[ext=mp4]/best"
    ),
    HD_720(
        label = "HD 720p",
        subtitle = "পরিষ্কার HD ভিডিও",
        formatSelector = "best[height<=720][ext=mp4]/best[height<=720]/best"
    ),
    SD_480(
        label = "SD 480p",
        subtitle = "কম ডেটায় দ্রুত ডাউনলোড",
        formatSelector = "best[height<=480][ext=mp4]/best[height<=480]/best"
    ),
    AUDIO_M4A(
        label = "শুধু অডিও (M4A)",
        subtitle = "সরাসরি অডিও ফাইল",
        formatSelector = "bestaudio[ext=m4a]/bestaudio"
    )
}

data class VideoMetadata(
    val title: String,
    val uploader: String?,
    val durationSec: Int,
    val thumbnailUrl: String?
)

data class DownloadedItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data object FetchingInfo : DownloadStatus
    data class Downloading(val progress: Float, val etaSeconds: Long, val logLine: String) : DownloadStatus
    data class Success(val message: String) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}
