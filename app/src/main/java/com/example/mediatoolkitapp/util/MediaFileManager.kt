package com.example.mediatoolkitapp.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaFileManager(
    private val context: Context
) {

    suspend fun copyUriToCache(uri: Uri): File = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri) ?: "selected_video_${System.currentTimeMillis()}.mp4"
        val outputFile = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot open selected video")

        outputFile
    }

    fun createTempOutputVideoFile(prefix: String = "processed_video"): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir

        if (!moviesDir.exists()) {
            moviesDir.mkdirs()
        }

        return File(
            moviesDir,
            "${prefix}_${System.currentTimeMillis()}.mp4"
        )
    }

    fun createTempOutputAudioFile(): File {
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir

        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }

        return File(
            musicDir,
            "extracted_audio_${System.currentTimeMillis()}.m4a"
        )
    }

    fun getVideoDurationMs(path: String): Long {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(path)

            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    suspend fun saveVideoToGallery(sourceFile: File): Uri = withContext(Dispatchers.IO) {
        val fileName = "MediaToolKit_${System.currentTimeMillis()}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/MediaToolKit"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver

        val videoUri = resolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create video in gallery")

        resolver.openOutputStream(videoUri)?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Failed to save video to gallery")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(videoUri, contentValues, null, null)
        }

        videoUri
    }

    suspend fun saveAudioToMusic(sourceFile: File): Uri = withContext(Dispatchers.IO) {
        val fileName = "MediaToolKit_Audio_${System.currentTimeMillis()}.m4a"
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MUSIC}/MediaToolKit"
                )
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver

        val audioUri = resolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create audio file")

        resolver.openOutputStream(audioUri)?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Failed to save audio file")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(audioUri, contentValues, null, null)
        }

        audioUri
    }

    suspend fun deleteMediaByUri(uriString: String): Boolean = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        context.contentResolver.delete(uri, null, null) > 0
    }

    private fun getFileName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }
}