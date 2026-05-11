package com.example.mediatoolkitapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.mediatoolkitapp.data.local.AppDatabase
import com.example.mediatoolkitapp.data.repository.MediaRepositoryImpl
import com.example.mediatoolkitapp.databinding.ActivityMainBinding
import com.example.mediatoolkitapp.media.MediaProcessor
import com.example.mediatoolkitapp.presentation.MediaUiState
import com.example.mediatoolkitapp.presentation.MediaViewModel
import com.example.mediatoolkitapp.presentation.MediaViewModelFactory
import com.example.mediatoolkitapp.util.MediaFileManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var player: ExoPlayer? = null

    private val viewModel: MediaViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = MediaRepositoryImpl(database.mediaHistoryDao())
        val mediaProcessor = MediaProcessor()
        val fileManager = MediaFileManager(applicationContext)

        MediaViewModelFactory(
            repository = repository,
            mediaProcessor = mediaProcessor,
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

        binding.btnExtractAudio.setOnClickListener {
            viewModel.extractAudioFromSelectedVideo()
        }

        binding.btnMuteVideo.setOnClickListener {
            viewModel.muteSelectedVideo()
        }

        binding.btnCompressVideo.setOnClickListener {
            viewModel.compressSelectedVideo()
        }

        binding.btnProcessedFiles.setOnClickListener {
            startActivity(
                Intent(this, ProcessedFilesActivity::class.java)
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: MediaUiState) {
        when (state) {
            MediaUiState.Idle -> {
                binding.tvStatus.text = "Status: No video selected"
                hideProgress()
                setControlsEnabled(true)
            }

            is MediaUiState.VideoSelected -> {
                binding.tvStatus.text = "Status: Video selected\nFile: ${state.inputName}"
                hideProgress()
                setControlsEnabled(true)
            }

            is MediaUiState.Processing -> {
                binding.tvStatus.text = "Status: ${state.message}"
                setControlsEnabled(false)

                if (state.progress != null) {
                    showProgress(state.progress)
                } else {
                    hideProgress()
                }
            }

            is MediaUiState.Success -> {
                binding.tvStatus.text = "Status: ${state.message}"
                hideProgress()
                setControlsEnabled(true)

                if (state.outputMimeType?.startsWith("video") == true) {
                    playVideo(Uri.parse(state.outputPath))
                }
            }

            is MediaUiState.Error -> {
                binding.tvStatus.text = "Status: Error\n${state.message}"
                hideProgress()
                setControlsEnabled(true)
            }
        }
    }

    private fun showProgress(progress: Int) {
        binding.progressProcessing.visibility = View.VISIBLE
        binding.tvProgressPercent.visibility = View.VISIBLE

        binding.progressProcessing.progress = progress
        binding.tvProgressPercent.text = "$progress%"
    }

    private fun hideProgress() {
        binding.progressProcessing.visibility = View.GONE
        binding.tvProgressPercent.visibility = View.GONE

        binding.progressProcessing.progress = 0
        binding.tvProgressPercent.text = "0%"
    }

    private fun setControlsEnabled(enabled: Boolean) {
        binding.btnSelectVideo.isEnabled = enabled
        binding.btnTrimVideo.isEnabled = enabled
        binding.btnExtractAudio.isEnabled = enabled
        binding.btnMuteVideo.isEnabled = enabled
        binding.btnCompressVideo.isEnabled = enabled
        binding.btnProcessedFiles.isEnabled = enabled
        binding.etStartSeconds.isEnabled = enabled
        binding.etEndSeconds.isEnabled = enabled
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