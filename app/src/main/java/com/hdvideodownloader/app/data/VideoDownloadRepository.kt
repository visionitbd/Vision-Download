package com.hdvideodownloader.app.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.hdvideodownloader.app.model.DownloadedItem
import com.hdvideodownloader.app.model.QualityOption
import com.hdvideodownloader.app.model.VideoMetadata
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoDownloadRepository(private val context: Context) {

    fun getOutputDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val appDir = File(downloadsDir, "VisionDownload")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }

    suspend fun fetchVideoMetadata(url: String): VideoMetadata = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
        }
        val info = YoutubeDL.getInstance().getInfo(request)
        VideoMetadata(
            title = info.title ?: "Untitled Video",
            uploader = info.uploader,
            durationSec = info.duration,
            thumbnailUrl = info.thumbnail
        )
    }

    suspend fun downloadVideo(
        url: String,
        quality: QualityOption,
        processId: String,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val outputDir = getOutputDirectory()
        val beforeFiles = outputDir.listFiles()?.map { it.absolutePath }?.toSet() ?: emptySet()

        val request = YoutubeDLRequest(url).apply {
            addOption("--no-mtime")
            addOption("--no-playlist")
            addOption("--restrict-filenames")
            addOption("-f", quality.formatSelector)
            addOption("-o", "${outputDir.absolutePath}/%(title).70s_%(id)s.%(ext)s")
        }

        YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
            onProgress(progress, etaInSeconds, line)
        }

        val afterFiles = outputDir.listFiles()
            ?.filter { it.isFile && it.absolutePath !in beforeFiles }
            ?: emptyList()

        if (afterFiles.isNotEmpty()) {
            MediaScannerConnection.scanFile(
                context,
                afterFiles.map { it.absolutePath }.toTypedArray(),
                null,
                null
            )
        }
        afterFiles
    }

    fun cancelDownload(processId: String) {
        YoutubeDL.getInstance().destroyProcessById(processId)
    }

    suspend fun updateEngine(): YoutubeDL.UpdateStatus? = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
    }

    suspend fun listDownloadedFiles(): List<DownloadedItem> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = getOutputDirectory()
            dir.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    DownloadedItem(
                        file = file,
                        name = file.name,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified()
                    )
                }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
