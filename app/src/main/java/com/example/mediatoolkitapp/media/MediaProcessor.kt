package com.example.mediatoolkitapp.media

import android.util.Log
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MediaProcessor {

    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startSeconds: Long,
        endSeconds: Long
    ): MediaProcessResult = withContext(Dispatchers.IO) {

        if (startSeconds < 0) {
            return@withContext MediaProcessResult.Error("Start time cannot be negative")
        }

        if (endSeconds <= startSeconds) {
            return@withContext MediaProcessResult.Error("End time must be greater than start time")
        }

        val duration = endSeconds - startSeconds

        val command = buildString {
            append("-y ")
            append("-ss $startSeconds ")
            append("-i ${quote(inputPath)} ")
            append("-t $duration ")
            append("-c copy ")
            append(quote(outputPath))
        }

        executeCommand(
            command = command,
            outputPath = outputPath,
            userErrorMessage = "Failed to trim video. Please try another video."
        )
    }

    suspend fun extractAudio(
        inputPath: String,
        outputPath: String
    ): MediaProcessResult = withContext(Dispatchers.IO) {

        val command = buildString {
            append("-y ")
            append("-i ${quote(inputPath)} ")
            append("-vn ")
            append("-c:a aac ")
            append("-b:a 128k ")
            append(quote(outputPath))
        }

        executeCommand(
            command = command,
            outputPath = outputPath,
            userErrorMessage = "Failed to extract audio. Please try another video."
        )
    }

    suspend fun muteVideo(
        inputPath: String,
        outputPath: String
    ): MediaProcessResult = withContext(Dispatchers.IO) {

        val command = buildString {
            append("-y ")
            append("-i ${quote(inputPath)} ")
            append("-an ")
            append("-c:v copy ")
            append(quote(outputPath))
        }

        executeCommand(
            command = command,
            outputPath = outputPath,
            userErrorMessage = "Failed to mute video. Please try another video."
        )
    }

    suspend fun compressVideo(
        inputPath: String,
        outputPath: String,
        totalDurationMs: Long,
        onProgress: (Int) -> Unit
    ): MediaProcessResult = withContext(Dispatchers.IO) {

        val command = buildString {
            append("-y ")
            append("-i ${quote(inputPath)} ")
            append("-c:v mpeg4 ")
            append("-q:v 7 ")
            append("-c:a aac ")
            append("-b:a 96k ")
            append("-movflags +faststart ")
            append(quote(outputPath))
        }

        executeCommandWithProgress(
            command = command,
            outputPath = outputPath,
            totalDurationMs = totalDurationMs,
            onProgress = onProgress,
            userErrorMessage = "Failed to compress video. Please try another video."
        )
    }

    private fun executeCommand(
        command: String,
        outputPath: String,
        userErrorMessage: String
    ): MediaProcessResult {
        Log.d("MediaProcessor", "FFmpeg command: $command")

        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode

        return if (ReturnCode.isSuccess(returnCode)) {
            MediaProcessResult.Success(outputPath)
        } else {
            val logs = session.allLogsAsString
            Log.e("MediaProcessor", logs)

            MediaProcessResult.Error(userErrorMessage)
        }
    }

    private suspend fun executeCommandWithProgress(
        command: String,
        outputPath: String,
        totalDurationMs: Long,
        onProgress: (Int) -> Unit,
        userErrorMessage: String
    ): MediaProcessResult = suspendCancellableCoroutine { continuation ->

        Log.d("MediaProcessor", "FFmpeg command: $command")

        val session = FFmpegKit.executeAsync(
            command,
            { completedSession ->
                val returnCode = completedSession.returnCode

                if (ReturnCode.isSuccess(returnCode)) {
                    onProgress(100)

                    if (continuation.isActive) {
                        continuation.resume(MediaProcessResult.Success(outputPath))
                    }
                } else {
                    val logs = completedSession.allLogsAsString
                    Log.e("MediaProcessor", logs)

                    if (continuation.isActive) {
                        continuation.resume(
                            MediaProcessResult.Error(userErrorMessage)
                        )
                    }
                }
            },
            { log ->
                Log.d("MediaProcessor", log.message)
            },
            { statistics ->
                if (totalDurationMs > 0) {
                    val currentTimeMs = statistics.time
                    val progress = ((currentTimeMs.toDouble() / totalDurationMs) * 100)
                        .toInt()
                        .coerceIn(0, 99)

                    onProgress(progress)
                }
            }
        )

        continuation.invokeOnCancellation {
            session.cancel()
        }
    }

    private fun quote(path: String): String {
        return "\"${path.replace("\"", "\\\"")}\""
    }
}

sealed class MediaProcessResult {
    data class Success(val outputPath: String) : MediaProcessResult()
    data class Error(val message: String) : MediaProcessResult()
}