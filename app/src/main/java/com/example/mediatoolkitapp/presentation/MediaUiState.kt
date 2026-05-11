package com.example.mediatoolkitapp.presentation

import android.net.Uri

sealed class MediaUiState {
    data object Idle : MediaUiState()

    data class VideoSelected(
        val uri: Uri,
        val cachedInputPath: String,
        val inputName: String
    ) : MediaUiState()

    data class Processing(
        val message: String,
        val progress: Int? = null
    ) : MediaUiState()

    data class Success(
        val message: String,
        val outputPath: String,
        val outputMimeType: String?
    ) : MediaUiState()

    data class Error(
        val message: String
    ) : MediaUiState()
}