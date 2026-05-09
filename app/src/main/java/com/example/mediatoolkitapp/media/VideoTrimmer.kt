package com.example.mediatoolkitapp.media

import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.ReturnCode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoTrimmer {

    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startSeconds: Long,
        endSeconds: Long
    ): TrimResult = withContext(Dispatchers.IO) {

        if (startSeconds < 0) {
            return@withContext TrimResult.Error("Start time cannot be negative")
        }

        if (endSeconds <= startSeconds) {
            return@withContext TrimResult.Error("End time must be greater than start time")
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

        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            TrimResult.Success(outputPath)
        } else {
            val logs = session.allLogsAsString
            TrimResult.Error(
                message = logs.ifBlank { "FFmpeg trimming failed" }
            )
        }
    }

    private fun quote(path: String): String {
        return "\"${path.replace("\"", "\\\"")}\""
    }
}

sealed class TrimResult {
    data class Success(val outputPath: String) : TrimResult()
    data class Error(val message: String) : TrimResult()
}