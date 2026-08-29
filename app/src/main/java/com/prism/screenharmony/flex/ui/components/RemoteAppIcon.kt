package com.prism.screenharmony.flex.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * Universal Remote/Local App Icon component implementing:
 * 1. Step 1: Instant Local PackageManager load if app is on this device (0ms, 0 network).
 * 2. Step 2 (Option B): Google Play icon resolution with persistent local disk cache.
 * 3. Step 3 (Option A): Dynamic Material 3 monogram badge with deterministic color generation.
 */
@Composable
fun RemoteAppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    var localBitmap by remember(packageName) { mutableStateOf<Bitmap?>(null) }
    var cachedIconFile by remember(packageName) { mutableStateOf<File?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            // 1. Try local PackageManager first (0ms, zero network)
            try {
                val pm = context.packageManager
                val drawable = pm.getApplicationIcon(packageName)
                val bmp = drawableToBitmap(drawable)
                localBitmap = bmp
                return@withContext
            } catch (_: Exception) {
                // Not installed on this phone
            }

            // 2. Check local disk cache (Option B)
            val cacheDir = File(context.cacheDir, "remote_app_icons").apply { mkdirs() }
            val iconFile = File(cacheDir, "${packageName.replace('.', '_')}.png")
            if (iconFile.exists() && iconFile.length() > 0) {
                cachedIconFile = iconFile
                return@withContext
            }

            // 3. Background download and cache Google Play icon (Option B)
            downloadAndCachePlayIcon(packageName, iconFile) { downloadedFile ->
                cachedIconFile = downloadedFile
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Local native bitmap loaded
            localBitmap != null -> {
                Image(
                    bitmap = localBitmap!!.asImageBitmap(),
                    contentDescription = appName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Cached local disk file loaded via Coil (Option B)
            cachedIconFile != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(cachedIconFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = appName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallback = null,
                    error = null
                )
            }
            // Fallback: Dynamic Material 3 Initials & Category Badge (Option A)
            else -> {
                AppInitialsBadge(
                    appName = appName,
                    packageName = packageName,
                    size = size
                )
            }
        }
    }
}

/**
 * Option A: Material 3 Dynamic Monogram / Category Badge
 * Generates harmonic container colors deterministically from the package name hash.
 */
@Composable
fun AppInitialsBadge(
    appName: String,
    packageName: String,
    size: Dp = 40.dp
) {
    val initials = remember(appName) {
        extractAppInitials(appName)
    }

    val (bgColor, textColor) = remember(packageName) {
        generateDeterministicM3Colors(packageName)
    }

    Surface(
        color = bgColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (initials.isNotBlank()) {
                Text(
                    text = initials,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

private fun extractAppInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return ""
    val parts = clean.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
    } else {
        clean.take(2).uppercase()
    }
}

private fun generateDeterministicM3Colors(packageName: String): Pair<Color, Color> {
    val hash = abs(packageName.hashCode())
    val palettes = listOf(
        Pair(Color(0xFFE8DEF8), Color(0xFF1D192B)), // Purple
        Pair(Color(0xFFD0E4FF), Color(0xFF001D36)), // Blue
        Pair(Color(0xFFC4EED0), Color(0xFF072111)), // Green
        Pair(Color(0xFFFFD8E4), Color(0xFF31111D)), // Pink
        Pair(Color(0xFFFFDCC2), Color(0xFF2E1500)), // Orange
        Pair(Color(0xFFCCE8E8), Color(0xFF002022)), // Teal
        Pair(Color(0xFFE2E2E9), Color(0xFF191C1E))  // Neutral
    )
    return palettes[hash % palettes.size]
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/**
 * Option B: Resolves Google Play Store icon URL and stores it permanently in local disk cache.
 */
private suspend fun downloadAndCachePlayIcon(
    packageName: String,
    targetFile: File,
    onSuccess: (File) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val storeUrl = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
            val conn = URL(storeUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0")

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                val iconUrlRegex = Regex("""src="(https://play-lh\.googleusercontent\.com/[^"]+)"""")
                val match = iconUrlRegex.find(html)
                val iconUrl = match?.groupValues?.get(1)?.replace("&amp;", "&")

                if (!iconUrl.isNullOrBlank()) {
                    val iconConn = URL(iconUrl).openConnection() as HttpURLConnection
                    iconConn.connectTimeout = 4000
                    iconConn.readTimeout = 4000
                    if (iconConn.responseCode == 200) {
                        val bmp = BitmapFactory.decodeStream(iconConn.inputStream)
                        if (bmp != null) {
                            FileOutputStream(targetFile).use { out ->
                                bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                            }
                            withContext(Dispatchers.Main) {
                                onSuccess(targetFile)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handled silently -> Option A fallback remains seamlessly visible
        }
    }
}
