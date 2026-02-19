package com.porarrirr.sumahohikakuku.ui

import android.content.ContentValues
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.porarrirr.sumahohikakuku.ui.theme.SumahohikakukuTheme
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Context.findComponentActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

internal suspend fun renderComposableToBitmap(
    activity: ComponentActivity,
    widthPx: Int,
    content: @Composable () -> Unit
): Bitmap = withContext(Dispatchers.Main) {
    val root = activity.findViewById<ViewGroup>(android.R.id.content)
    val composeView = ComposeView(activity).apply {
        setContent {
            SumahohikakukuTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        }
    }

    root.addView(
        composeView,
        ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
    )

    try {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            composeView.measuredWidth,
            composeView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        bitmap
    } finally {
        root.removeView(composeView)
    }
}

internal suspend fun saveBitmapToPictures(
    context: Context,
    bitmap: Bitmap,
    displayName: String,
    relativePath: String = "Pictures/Sumahohikakuku"
): Uri = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException("Failed to create MediaStore record.")

    try {
        resolver.openOutputStream(uri)?.use { out ->
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!ok) throw IllegalStateException("Failed to compress bitmap.")
        } ?: throw IllegalStateException("Failed to open MediaStore output stream.")

        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }.also { updated ->
            resolver.update(uri, updated, null, null)
        }
        uri
    } catch (e: Throwable) {
        runCatching { resolver.delete(uri, null, null) }
        throw e
    }
}

internal suspend fun saveBitmapToShareCache(
    context: Context,
    bitmap: Bitmap,
    displayName: String
): Uri = withContext(Dispatchers.IO) {
    val cacheDirectory = File(context.cacheDir, "shared_images").apply {
        mkdirs()
    }
    val imageFile = File(cacheDirectory, displayName)
    FileOutputStream(imageFile).use { output ->
        val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        if (!ok) throw IllegalStateException("Failed to compress bitmap.")
    }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

internal fun fitBitmapToAspectRatio(
    source: Bitmap,
    aspectWidth: Int,
    aspectHeight: Int
): Bitmap {
    val safeWidth = aspectWidth.coerceAtLeast(1)
    val safeHeight = aspectHeight.coerceAtLeast(1)
    val targetRatio = safeWidth.toFloat() / safeHeight.toFloat()
    val sourceRatio = source.width.toFloat() / source.height.toFloat()
    if (abs(sourceRatio - targetRatio) < 0.0001f) {
        return source
    }

    val targetWidth: Int
    val targetHeight: Int
    if (sourceRatio > targetRatio) {
        targetWidth = source.width
        targetHeight = (source.width / targetRatio).roundToInt().coerceAtLeast(1)
    } else {
        targetHeight = source.height
        targetWidth = (source.height * targetRatio).roundToInt().coerceAtLeast(1)
    }

    return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also { canvasBitmap ->
        val canvas = Canvas(canvasBitmap)
        val backgroundColor = source.getPixel(0, 0)
        canvas.drawColor(backgroundColor)
        val left = (targetWidth - source.width) / 2f
        val top = (targetHeight - source.height) / 2f
        canvas.drawBitmap(source, left, top, null)
    }
}

internal fun shareImages(context: Context, uris: List<Uri>, chooserTitle: String) {
    require(uris.isNotEmpty()) { "No image URIs to share." }

    val clipData = ClipData.newUri(context.contentResolver, chooserTitle, uris.first()).apply {
        uris.drop(1).forEach { uri ->
            addItem(ClipData.Item(uri))
        }
    }

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/png"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        this.clipData = clipData
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
