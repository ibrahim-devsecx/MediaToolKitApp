package com.example.mediatoolkitapp.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import com.example.mediatoolkitapp.data.repository.MediaRepository
import com.example.mediatoolkitapp.media.MediaProcessResult
import com.example.mediatoolkitapp.media.MediaProcessor
import com.example.mediatoolkitapp.util.MediaFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(
    private val repository: MediaRepository,
    private val mediaProcessor: MediaProcessor,
    private val mediaFileManager: MediaFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Idle)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    val history = repository.observeHistory()
    val processedFiles = repository.observeProcessedFiles()

    private var selectedVideo: SelectedVideo? = null

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = MediaUiState.Processing("Preparing selected video...")

                val cachedFile = mediaFileManager.copyUriToCache(uri)

                selectedVideo = SelectedVideo(
                    originalUri = uri,
                    cachedInputPath = cachedFile.absolutePath,
                    inputName = cachedFile.name
                )

                _uiState.value = MediaUiState.VideoSelected(
                    uri = uri,
                    cachedInputPath = cachedFile.absolutePath,
                    inputName = cachedFile.name
                )
            } catch (e: Exception) {
                _uiState.value = MediaUiState.Error(
                    e.message ?: "Failed to prepare selected video"
                )
            }
        }
    }

    fun trimSelectedVideo(
        startSeconds: Long,
        endSeconds: Long
    ) {
        val video = selectedVideo

        if (video == null) {
            _uiState.value = MediaUiState.Error("Please select a video first")
            return
        }

        viewModelScope.launch {
            _uiState.value = MediaUiState.Processing("Trimming video...")

            val outputFile = mediaFileManager.createTempOutputVideoFile("trimmed_video")

            when (
                val result = mediaProcessor.trimVideo(
                    inputPath = video.cachedInputPath,
                    outputPath = outputFile.absolutePath,
                    startSeconds = startSeconds,
                    endSeconds = endSeconds
                )
            ) {
                is MediaProcessResult.Success -> {
                    val galleryUri = mediaFileManager.saveVideoToGallery(
                        sourceFile = File(result.outputPath)
                    )

                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "TRIM_VIDEO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = galleryUri.toString(),
                            outputMimeType = "video/mp4",
                            startSeconds = startSeconds,
                            endSeconds = endSeconds,
                            status = "SUCCESS",
                            message = "Video trimmed and saved to gallery"
                        )
                    )

                    _uiState.value = MediaUiState.Success(
                        message = "Trim completed and saved to gallery",
                        outputPath = galleryUri.toString(),
                        outputMimeType = "video/mp4"
                    )
                }

                is MediaProcessResult.Error -> {
                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "TRIM_VIDEO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = null,
                            outputMimeType = null,
                            startSeconds = startSeconds,
                            endSeconds = endSeconds,
                            status = "FAILED",
                            message = result.message
                        )
                    )

                    _uiState.value = MediaUiState.Error(result.message)
                }
            }
        }
    }

    fun extractAudioFromSelectedVideo() {
        val video = selectedVideo

        if (video == null) {
            _uiState.value = MediaUiState.Error("Please select a video first")
            return
        }

        viewModelScope.launch {
            _uiState.value = MediaUiState.Processing("Extracting audio...")

            val outputFile = mediaFileManager.createTempOutputAudioFile()

            when (
                val result = mediaProcessor.extractAudio(
                    inputPath = video.cachedInputPath,
                    outputPath = outputFile.absolutePath
                )
            ) {
                is MediaProcessResult.Success -> {
                    val audioUri = mediaFileManager.saveAudioToMusic(
                        sourceFile = File(result.outputPath)
                    )

                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "EXTRACT_AUDIO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = audioUri.toString(),
                            outputMimeType = "audio/mp4",
                            startSeconds = null,
                            endSeconds = null,
                            status = "SUCCESS",
                            message = "Audio extracted and saved to Music"
                        )
                    )

                    _uiState.value = MediaUiState.Success(
                        message = "Audio extracted and saved to Music",
                        outputPath = audioUri.toString(),
                        outputMimeType = "audio/mp4"
                    )
                }

                is MediaProcessResult.Error -> {
                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "EXTRACT_AUDIO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = null,
                            outputMimeType = null,
                            startSeconds = null,
                            endSeconds = null,
                            status = "FAILED",
                            message = result.message
                        )
                    )

                    _uiState.value = MediaUiState.Error(result.message)
                }
            }
        }
    }

    fun muteSelectedVideo() {
        val video = selectedVideo

        if (video == null) {
            _uiState.value = MediaUiState.Error("Please select a video first")
            return
        }

        viewModelScope.launch {
            _uiState.value = MediaUiState.Processing("Removing audio from video...")

            val outputFile = mediaFileManager.createTempOutputVideoFile("muted_video")

            when (
                val result = mediaProcessor.muteVideo(
                    inputPath = video.cachedInputPath,
                    outputPath = outputFile.absolutePath
                )
            ) {
                is MediaProcessResult.Success -> {
                    val galleryUri = mediaFileManager.saveVideoToGallery(
                        sourceFile = File(result.outputPath)
                    )

                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "MUTE_VIDEO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = galleryUri.toString(),
                            outputMimeType = "video/mp4",
                            startSeconds = null,
                            endSeconds = null,
                            status = "SUCCESS",
                            message = "Video muted and saved to gallery"
                        )
                    )

                    _uiState.value = MediaUiState.Success(
                        message = "Video muted and saved to gallery",
                        outputPath = galleryUri.toString(),
                        outputMimeType = "video/mp4"
                    )
                }

                is MediaProcessResult.Error -> {
                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "MUTE_VIDEO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = null,
                            outputMimeType = null,
                            startSeconds = null,
                            endSeconds = null,
                            status = "FAILED",
                            message = result.message
                        )
                    )

                    _uiState.value = MediaUiState.Error(result.message)
                }
            }
        }
    }

    fun compressSelectedVideo() {
        val video = selectedVideo

        if (video == null) {
            _uiState.value = MediaUiState.Error("Please select a video first")
            return
        }

        viewModelScope.launch {
            _uiState.value = MediaUiState.Processing(
                message = "Compressing video...",
                progress = 0
            )

            val outputFile = mediaFileManager.createTempOutputVideoFile("compressed_video")
            val durationMs = mediaFileManager.getVideoDurationMs(video.cachedInputPath)

            when (
                val result = mediaProcessor.compressVideo(
                    inputPath = video.cachedInputPath,
                    outputPath = outputFile.absolutePath,
                    totalDurationMs = durationMs,
                    onProgress = { progress ->
                        _uiState.value = MediaUiState.Processing(
                            message = "Compressing video...",
                            progress = progress
                        )
                    }
                )
            ) {
                is MediaProcessResult.Success -> {
                    val galleryUri = mediaFileManager.saveVideoToGallery(
                        sourceFile = File(result.outputPath)
                    )

                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "COMPRESS_VIDEO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = galleryUri.toString(),
                            outputMimeType = "video/mp4",
                            startSeconds = null,
                            endSeconds = null,
                            status = "SUCCESS",
                            message = "Video compressed and saved to gallery"
                        )
                    )

                    _uiState.value = MediaUiState.Success(
                        message = "Video compressed and saved to gallery",
                        outputPath = galleryUri.toString(),
                        outputMimeType = "video/mp4"
                    )
                }

                is MediaProcessResult.Error -> {
                    repository.saveHistory(
                        MediaHistoryEntity(
                            operationType = "COMPRESS_VIDEO",
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = null,
                            outputMimeType = null,
                            startSeconds = null,
                            endSeconds = null,
                            status = "FAILED",
                            message = result.message
                        )
                    )

                    _uiState.value = MediaUiState.Error(result.message)
                }
            }
        }
    }

    fun deleteProcessedItem(item: MediaHistoryEntity) {
        viewModelScope.launch {
            try {
                val outputPath = item.outputPath

                if (!outputPath.isNullOrBlank()) {
                    mediaFileManager.deleteMediaByUri(outputPath)
                }

                repository.deleteHistoryById(item.id)
            } catch (e: Exception) {
                _uiState.value = MediaUiState.Error(
                    e.message ?: "Failed to delete file"
                )
            }
        }
    }

    private data class SelectedVideo(
        val originalUri: Uri,
        val cachedInputPath: String,
        val inputName: String
    )
}

class MediaViewModelFactory(
    private val repository: MediaRepository,
    private val mediaProcessor: MediaProcessor,
    private val mediaFileManager: MediaFileManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MediaViewModel(
            repository = repository,
            mediaProcessor = mediaProcessor,
            mediaFileManager = mediaFileManager
        ) as T
    }
}