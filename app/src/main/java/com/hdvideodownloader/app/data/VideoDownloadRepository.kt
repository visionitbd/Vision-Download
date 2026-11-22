package com.hdvideodownloader.app.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.hdvideodownloader.app.model.DownloadedItem
import com.hdvideodownloader.app.model.QualityOption
import com.hdvideodownloader.app.model.VideoMetadata
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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

    suspend fun fetchVideoMetadata(
        url: String,
        quality: QualityOption
    ): VideoMetadata = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--no-warnings")
            addOption("--socket-timeout", "15")
            addOption("-f", quality.formatSelector)
        }
        val info = YoutubeDL.getInstance().getInfo(request)
        VideoMetadata(
            title = info.title ?: "Untitled Video",
            uploader = info.uploader,
            durationSec = info.duration,
            thumbnailUrl = info.thumbnail,
            streamUrl = info.url,
            ext = info.ext
        )
    }

    /**
     * High-speed native Android HTTP downloader (256 KB buffered stream).
     * Starts instantly when streamUrl is pre-fetched and streams at full device bandwidth.
     */
    suspend fun downloadDirectStream(
        streamUrl: String,
        title: String,
        ext: String?,
        onProgress: (progress: Float, etaSeconds: Long, speedText: String?) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val outputDir = getOutputDirectory()
        val safeTitle = title
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(65)
            .ifBlank { "VisionVideo" }
        val fileExt = (ext ?: "mp4").lowercase(Locale.ROOT).take(5)
        val finalFile = File(outputDir, "${safeTitle}_${System.currentTimeMillis() % 100000}.$fileExt")
        val partFile = File(outputDir, "${finalFile.name}.part")

        val conn = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Connection", "keep-alive")
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val totalBytes = conn.contentLengthLong
            if (totalBytes <= 0L) {
                throw IllegalStateException("Unknown content length")
            }

            val buffer = ByteArray(256 * 1024)
            var downloaded = 0L
            val startTime = System.currentTimeMillis()
            var lastEmit = 0L

            BufferedInputStream(conn.inputStream, 256 * 1024).use { input ->
                BufferedOutputStream(FileOutputStream(partFile), 256 * 1024).use { output ->
                    while (true) {
                        if (!currentCoroutineContext().isActive) {
                            throw CancellationException("Cancelled")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmit >= 120L || downloaded >= totalBytes) {
                            lastEmit = now
                            val pct = (downloaded * 100f / totalBytes).coerceIn(0f, 100f)
                            val elapsedSec = ((now - startTime) / 1000.0).coerceAtLeast(0.1)
                            val bytesPerSec = downloaded / elapsedSec
                            val etaSec = if (bytesPerSec > 0) {
                                ((totalBytes - downloaded) / bytesPerSec).toLong().coerceAtLeast(0L)
                            } else 0L
                            onProgress(pct, etaSec, formatSpeed(bytesPerSec))
                        }
                    }
                    output.flush()
                }
            }

            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(finalFile.absolutePath),
                null,
                null
            )
            finalFile
        } catch (t: Throwable) {
            partFile.delete()
            throw t
        } finally {
            conn.disconnect()
        }
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
            addOption("--no-warnings")
            addOption("--restrict-filenames")
            addOption("--newline")
            addOption("--socket-timeout", "15")
            addOption("--concurrent-fragments", "8")
            addOption("--http-chunk-size", "10M")
            addOption("--buffer-size", "32K")
            addOption("--retries", "3")
            addOption("--fragment-retries", "3")
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

    suspend fun deleteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        runCatching { file.delete() }.getOrDefault(false)
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

    private fun formatSpeed(bytesPerSec: Double): String {
        val mb = bytesPerSec / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.ROOT, "%.1f MB/s", mb)
        } else {
            val kb = bytesPerSec / 1024.0
            String.format(Locale.ROOT, "%.0f KB/s", kb)
        }
    }
}
