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
    val formatSelector: String,
    val audioOnly: Boolean = false
) {
    BEST_HD(
        label = "সর্বোচ্চ HD (MP4)",
        subtitle = "সেরা কোয়ালিটি (ওয়াটারমার্ক ছাড়া)",
        formatSelector = "bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best[ext=mp4]/best"
    ),
    FULL_HD_1080(
        label = "Full HD 1080p",
        subtitle = "YouTube / FB 1080p MP4",
        formatSelector = "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=1080]+bestaudio/best[height<=1080]"
    ),
    HD_720(
        label = "HD 720p",
        subtitle = "কম ডেটায় পরিষ্কার HD ভিডিও",
        formatSelector = "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=720]+bestaudio/best[height<=720]"
    ),
    AUDIO_MP3(
        label = "শুধু অডিও (MP3)",
        subtitle = "ভিডিও থেকে MP3 অডিও",
        formatSelector = "bestaudio/best",
        audioOnly = true
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
