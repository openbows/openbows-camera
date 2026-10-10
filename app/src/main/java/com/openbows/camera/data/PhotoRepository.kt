package com.openbows.camera.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoRepository(
    private val context: Context,
    private val dao: PhotoDao,
) {
    val photos: Flow<List<PhotoRecord>> = dao.observeAll()
    val latest: Flow<PhotoRecord?> = dao.observeLatest()

    suspend fun get(id: Long): PhotoRecord? = dao.getById(id)

    suspend fun saveJpeg(bitmap: Bitmap, remark: String = ""): PhotoRecord = withContext(Dispatchers.IO) {
        val takenAt = System.currentTimeMillis()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(takenAt))
        val suffix = remarkFileSuffix(remark)
        val name = if (suffix.isEmpty()) {
            "IMG_$stamp.jpg"
        } else {
            "IMG_${stamp}_$suffix.jpg"
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertMediaStore(bitmap, name, takenAt)
        } else {
            insertLegacyFile(bitmap, name, takenAt)
        }
        val record = PhotoRecord(
            uri = uri.toString(),
            displayName = name,
            takenAt = takenAt,
            width = bitmap.width,
            height = bitmap.height,
        )
        val id = dao.insert(record)
        record.copy(id = id)
    }

    /** 去掉非法文件名字符后，从末尾最多取 40 个字符。 */
    internal fun remarkFileSuffix(remark: String): String {
        val cleaned = remark
            .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
        if (cleaned.isEmpty()) return ""
        return cleaned.takeLast(40)
    }

    suspend fun delete(photo: PhotoRecord) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.delete(Uri.parse(photo.uri), null, null)
        }
        dao.delete(photo)
    }

    private fun insertMediaStore(bitmap: Bitmap, name: String, takenAt: Long): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, takenAt)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OpenbowsCamera")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                error("JPEG compress failed")
            }
        } ?: error("Cannot open output stream")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun insertLegacyFile(bitmap: Bitmap, name: String, takenAt: Long): Uri {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "OpenbowsCamera",
        )
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                error("JPEG compress failed")
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, takenAt)
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: Uri.fromFile(file)
    }
}
