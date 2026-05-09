package com.example.mediatoolkitapp.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import com.example.mediatoolkitapp.data.repository.MediaRepository
import com.example.mediatoolkitapp.media.TrimResult
import com.example.mediatoolkitapp.media.VideoTrimmer
import com.example.mediatoolkitapp.util.MediaFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel(
    private val repository: MediaRepository,
    private val videoTrimmer: VideoTrimmer,
    private val mediaFileManager: MediaFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Idle)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    val history = repository.observeHistory()

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

            val outputFile = mediaFileManager.createTempOutputVideoFile()
            when (
                val result = videoTrimmer.trimVideo(
                    inputPath = video.cachedInputPath,
                    outputPath = outputFile.absolutePath,
                    startSeconds = startSeconds,
                    endSeconds = endSeconds
                )
            ) {
                is TrimResult.Success -> {
                    val galleryUri = mediaFileManager.saveVideoToGallery(
                        sourceFile = java.io.File(result.outputPath)
                    )

                    repository.saveHistory(
                        MediaHistoryEntity(
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = galleryUri.toString(),
                            startSeconds = startSeconds,
                            endSeconds = endSeconds,
                            status = "SUCCESS",
                            message = "Video trimmed and saved to gallery"
                        )
                    )

                    _uiState.value = MediaUiState.Success(galleryUri.toString())
                }

                is TrimResult.Error -> {
                    repository.saveHistory(
                        MediaHistoryEntity(
                            inputName = video.inputName,
                            inputPath = video.cachedInputPath,
                            outputPath = null,
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

    private data class SelectedVideo(
        val originalUri: Uri,
        val cachedInputPath: String,
        val inputName: String
    )
}

class MediaViewModelFactory(
    private val repository: MediaRepository,
    private val videoTrimmer: VideoTrimmer,
    private val mediaFileManager: MediaFileManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MediaViewModel(
            repository = repository,
            videoTrimmer = videoTrimmer,
            mediaFileManager = mediaFileManager
        ) as T
    }
}