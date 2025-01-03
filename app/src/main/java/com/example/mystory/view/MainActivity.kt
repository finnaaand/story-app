package com.example.mystory.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mystory.R
import com.example.mystory.api.ApiConfig.apiService
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.session.SessionRepository
import com.example.mystory.helper.StoryAdapter
import com.example.mystory.helper.StoryLoadStateAdapter
import com.example.mystory.model.StoryRepository
import com.example.mystory.model.StoryViewModel
import com.example.mystory.model.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var storyRepository: StoryRepository
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var storyViewModel: StoryViewModel
    private lateinit var progressBar: ProgressBar
    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh stories when returning from AddStoryActivity
            storyViewModel.refreshStories()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sessionManager = SessionManager(this)
        sessionRepository = SessionRepository(sessionManager)
        storyRepository = StoryRepository(apiService)
        val factory = ViewModelFactory(storyRepository, sessionManager)
        storyViewModel = ViewModelProvider(this, factory)[StoryViewModel::class.java]
        progressBar = findViewById(R.id.progressBar)

        lifecycleScope.launch {
            if (!sessionManager.isLoggedIn()) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }
            setupRecyclerView()
            observeViewModel()
        }

        findViewById<ImageView>(R.id.action_logout).setOnClickListener {
            logoutUser()
        }

        val fabAddStory: FloatingActionButton = findViewById(R.id.fab_add_story)
        fabAddStory.setOnClickListener {
            val intent = Intent(this, AddStoryActivity::class.java)
            ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, fabAddStory, "fabTransition"
            )
            // Use startForResult instead of startActivity
            startForResult.launch(intent)
        }

        findViewById<ImageView>(R.id.maps).setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("location", "1.0,1.0")
            startActivity(intent)
        }

        setupTransitions()
    }

    private fun setupTransitions() {
        window.sharedElementEnterTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
        window.sharedElementExitTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
    }

    private fun logoutUser() {
        lifecycleScope.launch {
            sessionRepository.clearUserSession()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        val rvStoryList = findViewById<RecyclerView>(R.id.rv_story_list)
        rvStoryList.layoutManager = LinearLayoutManager(this)
        storyAdapter = StoryAdapter { story, imageView ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("story", story)
            }
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                imageView,
                "storyImage"
            )
            startActivity(intent, options.toBundle())
        }

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh?.setOnRefreshListener {
            storyAdapter.refresh()
            swipeRefresh.isRefreshing = false
        }

        rvStoryList.adapter = storyAdapter.withLoadStateFooter(
            footer = StoryLoadStateAdapter { storyAdapter.retry() }
        )

        storyAdapter.addLoadStateListener { loadState ->
            progressBar.isVisible = loadState.source.refresh is LoadState.Loading
            val errorState = loadState.source.append as? LoadState.Error
                ?: loadState.source.prepend as? LoadState.Error
                ?: loadState.append as? LoadState.Error
                ?: loadState.prepend as? LoadState.Error

            errorState?.let {
                Toast.makeText(this, "Error: ${it.error.message}", Toast.LENGTH_LONG).show()
            }

            val isEmpty = loadState.source.refresh is LoadState.NotLoading &&
                    storyAdapter.itemCount == 0
            if (isEmpty) {
                Toast.makeText(this, "No stories available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            val token = sessionRepository.getUserToken()
            Log.d("MainActivity", "Token: $token")

            storyViewModel.getAllStories("Bearer $token")
                .collectLatest { pagingData ->
                    Log.d("MainActivity", "Received new paging data")
                    storyAdapter.submitData(pagingData)
                }
        }
    }
}
