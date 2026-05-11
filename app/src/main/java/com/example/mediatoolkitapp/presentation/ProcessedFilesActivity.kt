package com.example.mediatoolkitapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediatoolkitapp.data.local.AppDatabase
import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import com.example.mediatoolkitapp.data.repository.MediaRepositoryImpl
import com.example.mediatoolkitapp.databinding.ActivityProcessedFilesBinding
import com.example.mediatoolkitapp.media.MediaProcessor
import com.example.mediatoolkitapp.presentation.MediaHistoryAdapter
import com.example.mediatoolkitapp.presentation.MediaViewModel
import com.example.mediatoolkitapp.presentation.MediaViewModelFactory
import com.example.mediatoolkitapp.util.MediaFileManager
import kotlinx.coroutines.launch

class ProcessedFilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcessedFilesBinding
    private lateinit var adapter: MediaHistoryAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessedFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeProcessedFiles()
    }

    private fun setupRecyclerView() {
        adapter = MediaHistoryAdapter(
            onOpenClick = { item ->
                openOutputFile(item)
            },
            onShareClick = { item ->
                shareOutputFile(item)
            },
            onDeleteClick = { item ->
                viewModel.deleteProcessedItem(item)
            }
        )

        binding.rvProcessedFiles.apply {
            layoutManager = LinearLayoutManager(this@ProcessedFilesActivity)
            adapter = this@ProcessedFilesActivity.adapter
        }
    }

    private fun observeProcessedFiles() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.processedFiles.collect { files ->
                    adapter.submitList(files)

                    binding.tvEmpty.text = if (files.isEmpty()) {
                        "No processed files yet"
                    } else {
                        ""
                    }
                }
            }
        }
    }

    private fun openOutputFile(item: MediaHistoryEntity) {
        val uriString = item.outputPath ?: return
        val uri = Uri.parse(uriString)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.outputMimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(
            Intent.createChooser(intent, "Open with")
        )
    }

    private fun shareOutputFile(item: MediaHistoryEntity) {
        val uriString = item.outputPath ?: return
        val uri = Uri.parse(uriString)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.outputMimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(
            Intent.createChooser(intent, "Share file")
        )
    }
}