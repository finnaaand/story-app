package com.example.mystory.view

import android.os.Bundle
import android.transition.TransitionInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mystory.R
import com.example.mystory.api.ApiConfig
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.story.Story
import com.example.mystory.data.story.StoryDetailResponse
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {

    private lateinit var story: Story
    private lateinit var sessionManager: SessionManager
    private lateinit var nameTextView: TextView
    private lateinit var photoImageView: ImageView
    private lateinit var descriptionTextView: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        sessionManager = SessionManager(this)

        story = intent.getSerializableExtra("story") as Story

        nameTextView = findViewById(R.id.tv_detail_name)
        photoImageView = findViewById(R.id.iv_detail_photo)
        descriptionTextView = findViewById(R.id.tv_detail_description)
        backButton = findViewById(R.id.btn_back)

        updateUI(story)

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        fetchStoryDetails(story.id)

        window.sharedElementEnterTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
    }

    private fun fetchStoryDetails(storyId: String) {
        lifecycleScope.launch {
            val token = "Bearer ${sessionManager.getToken()}"
            ApiConfig.apiService.getStoryDetail(token, storyId).enqueue(object :
                Callback<StoryDetailResponse> {
                override fun onResponse(
                    call: Call<StoryDetailResponse>,
                    response: Response<StoryDetailResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val detailStory = response.body()!!.story
                        updateUI(detailStory)
                    } else {
                        Toast.makeText(
                            this@DetailActivity,
                            "Failed to fetch story details",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onFailure(call: Call<StoryDetailResponse>, t: Throwable) {
                    Toast.makeText(
                        this@DetailActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun updateUI(story: Story) {
        nameTextView.text = story.name
        descriptionTextView.text = story.description
        Glide.with(this)
            .load(story.photoUrl)
            .into(photoImageView)
    }
}