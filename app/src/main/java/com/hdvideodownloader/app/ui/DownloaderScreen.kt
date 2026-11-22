package com.hdvideodownloader.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hdvideodownloader.app.model.DownloadStatus
import com.hdvideodownloader.app.model.DownloadedItem
import com.hdvideodownloader.app.model.QualityOption
import com.hdvideodownloader.app.model.SocialPlatform
import com.hdvideodownloader.app.ui.theme.BgDark
import com.hdvideodownloader.app.ui.theme.BorderSubtle
import com.hdvideodownloader.app.ui.theme.EmeraldGlow
import com.hdvideodownloader.app.ui.theme.TealAccent
import com.hdvideodownloader.app.ui.theme.TextPrimary
import com.hdvideodownloader.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun DownloaderScreen(viewModel: DownloaderViewModel) {
    val state by viewModel.uiState.collectAsState()
    val engineReady by viewModel.engineReady.collectAsState()
    val initError by viewModel.initError.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val isDownloading = state.status is DownloadStatus.Downloading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x3814B8A6), Color.Transparent),
                    center = Offset(140f, 380f),
                    radius = 700f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x3310B981), Color.Transparent),
                    center = Offset(950f, 1950f),
                    radius = 750f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Custom Top Header matching mockup
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vision Download",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "YouTube, Facebook ও TikTok — HD কোয়ালিটি",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Outlined pill Update button
                    Surface(
                        onClick = { viewModel.updateEngine() },
                        enabled = engineReady && !state.isUpdatingEngine,
                        shape = RoundedCornerShape(50),
                        color = Color(0x2214B8A6),
                        border = BorderStroke(1.2.dp, TealAccent)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isUpdatingEngine) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldGlow
                                )
                            } else {
                                Text(
                                    text = "আপডেট",
                                    color = EmeraldGlow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Engine init banner
            if (!engineReady || initError != null) {
                item {
                    GlassCard(borderColor = if (initError != null) Color(0xFFF87171) else TealAccent) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (initError == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldGlow
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ডাউনলোডার ইঞ্জিন প্রস্তুত হচ্ছে...",
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    text = "ত্রুটি: $initError",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            state.engineMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = EmeraldGlow,
                        fontSize = 12.sp
                    )
                }
            }

            // Main Glassmorphic Input & Quality Card
            item {
                GlassCard(
                    cornerRadius = 22.dp,
                    gradientColors = listOf(Color(0xCC16333B), Color(0xCC171F30))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // URL Input Field with integrated Paste / Clear button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x66131C2B))
                                .border(1.dp, Color(0x665B748C), RoundedCornerShape(14.dp))
                                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.url.isEmpty()) {
                                    Text(
                                        text = "YouTube, Facebook বা\nTikTok লিংক",
                                        color = TextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                BasicTextField(
                                    value = state.url,
                                    onValueChange = viewModel::onUrlChanged,
                                    singleLine = true,
                                    cursorBrush = SolidColor(EmeraldGlow),
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (state.url.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onUrlChanged("") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "মুছুন",
                                        tint = TextSecondary
                                    )
                                }
                            } else {
                                Surface(
                                    onClick = {
                                        clipboard.getText()?.text?.let { viewModel.onUrlChanged(it) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x88334155)
                                ) {
                                    Text(
                                        text = "📋 পেস্ট",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }

                        // Detected platform pill
                        state.detectedPlatform?.let { platform ->
                            val badgeColor = when (platform) {
                                SocialPlatform.FACEBOOK -> Color(0xFF3B82F6)
                                SocialPlatform.YOUTUBE -> Color(0xFFEF4444)
                                SocialPlatform.TIKTOK -> Color(0xFFEC4899)
                                SocialPlatform.OTHER -> TealAccent
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x551E293B),
                                border = BorderStroke(1.dp, Color(0x5564748B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "শনাক্তকৃত প্ল্যাটফর্ম: ",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = platform.displayName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "কোয়ালিটি নির্বাচন করুন",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 2x2 Quality Selector Grid matching mockup
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QualityPill(
                                    option = QualityOption.BEST_HD,
                                    selected = state.selectedQuality == QualityOption.BEST_HD,
                                    onClick = { viewModel.onQualitySelected(QualityOption.BEST_HD) },
                                    modifier = Modifier.weight(1f)
                                )
                                QualityPill(
                                    option = QualityOption.HD_720,
                                    selected = state.selectedQuality == QualityOption.HD_720,
                                    onClick = { viewModel.onQualitySelected(QualityOption.HD_720) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QualityPill(
                                    option = QualityOption.SD_480,
                                    selected = state.selectedQuality == QualityOption.SD_480,
                                    onClick = { viewModel.onQualitySelected(QualityOption.SD_480) },
                                    modifier = Modifier.weight(1f)
                                )
                                QualityPill(
                                    option = QualityOption.AUDIO_M4A,
                                    selected = state.selectedQuality == QualityOption.AUDIO_M4A,
                                    onClick = { viewModel.onQualitySelected(QualityOption.AUDIO_M4A) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Action Buttons Row: Outlined Preview + Glowing Emerald Download
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                onClick = {
                                    if (state.metadata != null && !state.metadata?.streamUrl.isNullOrBlank()) {
                                        viewModel.openPreviewPlayer()
                                    } else {
                                        viewModel.fetchPreview(autoPlay = true)
                                    }
                                },
                                enabled = engineReady && !isDownloading,
                                shape = RoundedCornerShape(50),
                                color = Color(0x441E293B),
                                border = BorderStroke(1.dp, Color(0x8864748B)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "👁 প্রিভিউ",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            val downloadBrush = if (engineReady && !isDownloading) {
                                Brush.horizontalGradient(listOf(Color(0xFF2AD4A5), Color(0xFF4AF2B8)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF334155)))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(downloadBrush)
                                    .clickable(enabled = engineReady && !isDownloading) {
                                        viewModel.startDownload()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⬇ ডাউনলোড করুন",
                                    color = if (engineReady && !isDownloading) Color(0xFF042F2E) else TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Video Preview Card (Tappable to Play Video!)
            state.metadata?.let { meta ->
                item {
                    GlassCard(
                        cornerRadius = 18.dp,
                        modifier = Modifier.clickable { viewModel.openPreviewPlayer() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 122.dp, height = 84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = meta.thumbnailUrl,
                                    contentDescription = meta.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xAA000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "প্লে করুন",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                if (meta.durationSec > 0) {
                                    Text(
                                        text = formatDuration(meta.durationSec),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(5.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xBB000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = meta.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                meta.uploader?.let {
                                    Text(
                                        text = it,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x3314B8A6),
                                    border = BorderStroke(1.dp, TealAccent)
                                ) {
                                    Text(
                                        text = "${state.selectedQuality.badgeText} • প্লে করতে ট্যাপ করুন",
                                        color = EmeraldGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status / Live 0% -> 100% Progress Card
            when (val status = state.status) {
                DownloadStatus.Idle -> Unit

                DownloadStatus.FetchingInfo -> {
                    item {
                        GlassCard(cornerRadius = 16.dp, borderColor = TealAccent) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldGlow
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ভিডিও প্রিভিউ লোড হচ্ছে...",
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                is DownloadStatus.Downloading -> {
                    item {
                        GlassCard(
                            cornerRadius = 18.dp,
                            borderColor = Color(0x8834E8B0),
                            gradientColors = listOf(Color(0xCC16333B), Color(0xCC122833))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${status.statusLabel}: ${status.progress.toInt()}%",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val details = buildList {
                                            status.speedText?.let { add("স্পিড: $it") }
                                            if (status.etaSeconds > 0) add("বাকি: ${status.etaSeconds} সে.")
                                        }.joinToString(" • ")
                                        if (details.isNotEmpty()) {
                                            Text(
                                                text = details,
                                                color = EmeraldGlow,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = viewModel::cancelDownload,
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x441E293B),
                                        border = BorderStroke(1.dp, Color(0x8864748B))
                                    ) {
                                        Text(
                                            text = "বাতিল",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                                        )
                                    }
                                }

                                // Glowing gradient progress bar (0% -> 100%)
                                val fraction = (status.progress / 100f).coerceIn(0.02f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0x55334155))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0EA5E9), EmeraldGlow)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                is DownloadStatus.Success -> {
                    item {
                        GlassCard(cornerRadius = 16.dp, borderColor = EmeraldGlow) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = status.message,
                                    color = EmeraldGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF0EA5E9), EmeraldGlow)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                is DownloadStatus.Error -> {
                    item {
                        GlassCard(
                            cornerRadius = 16.dp,
                            borderColor = Color(0xFFF87171),
                            gradientColors = listOf(Color(0xCC3B1820), Color(0xCC26141C))
                        ) {
                            Text(
                                text = status.message,
                                modifier = Modifier.padding(14.dp),
                                color = Color(0xFFFECACA),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Recent Downloads Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সাম্প্রতিক ডাউনলোড (${state.downloads.size})",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = viewModel::refreshDownloads) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "রিফ্রেশ",
                            tint = TextPrimary
                        )
                    }
                }
            }

            if (state.downloads.isEmpty()) {
                item {
                    Text(
                        text = "এখনো কোনো ভিডিও ডাউনলোড করা হয়নি।",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(state.downloads, key = { it.file.absolutePath }) { item ->
                    DownloadedRow(
                        item = item,
                        onPlay = { openMediaFile(context, item.file) },
                        onShare = { shareMediaFile(context, item.file) },
                        onDelete = { viewModel.deleteDownload(item) }
                    )
                }
            }
        }

        // In-App Playable Video Preview Dialog
        state.playingPreviewUrl?.let { streamUrl ->
            VideoPreviewPlayerDialog(
                title = state.playingPreviewTitle ?: "ভিডিও প্রিভিউ",
                streamUrl = streamUrl,
                onDismiss = { viewModel.closePreviewPlayer() }
            )
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    borderColor: Color = Color(0x4464748B),
    gradientColors: List<Color> = listOf(Color(0xCC182638), Color(0xCC131C2D)),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(gradientColors))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

@Composable
private fun QualityPill(
    option: QualityOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) EmeraldGlow else BorderSubtle
    val bgColor = if (selected) Color(0x3314B8A6) else Color(0x55131C2B)
    val textColor = if (selected) TextPrimary else TextSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.height(44.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option.label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DownloadedRow(
    item: DownloadedItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        cornerRadius = 16.dp,
        modifier = Modifier.clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalVideoThumbnail(
                file = item.file,
                modifier = Modifier
                    .size(width = 72.dp, height = 54.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val extLabel = item.file.extension.uppercase(Locale.ROOT).ifBlank { "MP4" }
                Text(
                    text = "${formatFileSize(item.sizeBytes)} • $extLabel",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "চালান",
                    tint = TextPrimary
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "অপশন",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("চালান") },
                        onClick = {
                            menuExpanded = false
                            onPlay()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("শেয়ার করুন") },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("মুছে ফেলুন") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalVideoThumbnail(file: File, modifier: Modifier = Modifier) {
    var thumbBitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath) {
        thumbBitmap = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val frame = retriever.getFrameAtTime(1_000_000L)
                retriever.release()
                frame?.let { Bitmap.createScaledBitmap(it, 180, 135, true) }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        thumbBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VideoPreviewPlayerDialog(
    title: String,
    streamUrl: String,
    onDismiss: () -> Unit
) {
    var isBuffering by remember { mutableStateOf(true) }
    var playError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            cornerRadius = 20.dp,
            borderColor = TealAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (playError == null) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(Uri.parse(streamUrl))
                                    setOnPreparedListener { mp ->
                                        isBuffering = false
                                        mp.isLooping = false
                                        start()
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        isBuffering = false
                                        playError = "এই লিংকের সরাসরি স্ট্রিম প্লে করা যায়নি—ডাউনলোড করে দেখুন।"
                                        true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isBuffering) {
                            CircularProgressIndicator(color = EmeraldGlow)
                        }
                    } else {
                        Text(
                            text = playError ?: "",
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun openMediaFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val ext = file.extension.lowercase(Locale.ROOT)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "video/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "ভিডিও ওপেন করুন"))
}

private fun shareMediaFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val ext = file.extension.lowercase(Locale.ROOT)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "video/*"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "ভিডিও শেয়ার করুন"))
}

private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.ROOT, "%02d:%02d", m, s)
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.ROOT, "%.2f MB", mb)
}
