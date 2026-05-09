package com.example.mediatoolkitapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.mediatoolkitapp.data.local.AppDatabase
import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import com.example.mediatoolkitapp.data.repository.MediaRepositoryImpl
import com.example.mediatoolkitapp.databinding.ActivityMainBinding
import com.example.mediatoolkitapp.media.VideoTrimmer
import com.example.mediatoolkitapp.presentation.MediaUiState
import com.example.mediatoolkitapp.presentation.MediaViewModel
import com.example.mediatoolkitapp.presentation.MediaViewModelFactory
import com.example.mediatoolkitapp.util.MediaFileManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null

    private val viewModel: MediaViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = MediaRepositoryImpl(database.mediaHistoryDao())
        val videoTrimmer = VideoTrimmer()
        val fileManager = MediaFileManager(applicationContext)

        MediaViewModelFactory(
            repository = repository,
            videoTrimmer = videoTrimmer,
            mediaFileManager = fileManager
        )
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            playVideo(it)
            viewModel.onVideoSelected(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayer()
        setupListeners()
        observeViewModel()
    }


    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
    }

    private fun setupListeners() {
        binding.btnSelectVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        binding.btnTrimVideo.setOnClickListener {
            val start = binding.etStartSeconds.text.toString().toLongOrNull()
            val end = binding.etEndSeconds.text.toString().toLongOrNull()

            if (start == null || end == null) {
                binding.tvStatus.text = "Status: Please enter valid start and end seconds"
                return@setOnClickListener
            }

            viewModel.trimSelectedVideo(
                startSeconds = start,
                endSeconds = end
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }

                launch {
                    viewModel.history.collect { history ->
                        renderHistory(history)
                    }
                }
            }
        }
    }

    private fun renderUiState(state: MediaUiState) {
        when (state) {
            MediaUiState.Idle -> {
                binding.tvStatus.text = "Status: No video selected"
            }

            is MediaUiState.VideoSelected -> {
                binding.tvStatus.text = "Status: Video selected\nFile: ${state.inputName}"
            }

            is MediaUiState.Processing -> {
                binding.tvStatus.text = "Status: ${state.message}"
            }

            is MediaUiState.Success -> {
                binding.tvStatus.text = "Status: Trim completed and saved to gallery\nOutput: ${state.outputPath}"
                playVideo(Uri.parse(state.outputPath))
            }

            is MediaUiState.Error -> {
                binding.tvStatus.text = "Status: Error\n${state.message}"
            }
        }
    }

    private fun renderHistory(history: List<MediaHistoryEntity>) {
        if (history.isEmpty()) {
            binding.tvHistory.text = "No history yet"
            return
        }

        binding.tvHistory.text = history.joinToString(separator = "\n\n") { item ->
            val date = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date(item.createdAt))

            buildString {
                append("File: ${item.inputName}\n")
                append("Range: ${item.startSeconds}s - ${item.endSeconds}s\n")
                append("Status: ${item.status}\n")
                append("Date: $date")

                if (!item.outputPath.isNullOrBlank()) {
                    append("\nOutput: ${item.outputPath}")
                }

                if (!item.message.isNullOrBlank()) {
                    append("\nMessage: ${item.message}")
                }
            }
        }
    }

    private fun playVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.player = null
        player?.release()
        player = null
    }
}