package com.example.mystory.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mystory.api.ApiService
import com.example.mystory.data.story.Story
import com.example.mystory.data.story.StoryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryRepository(
    val apiService: ApiService,
    ) {

    suspend fun getStoriesWithLocation(token: String): StoryResponse {
        return apiService.getStoriesWithLocation("Bearer $token")
    }

    fun getStoriesForWidget(token: String): LiveData<List<Story>> {
        val storiesLiveData = MutableLiveData<List<Story>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getAllStories(token)
                withContext(Dispatchers.Main) {
                    storiesLiveData.value = response.listStory
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("StoryRepository", "Error fetching stories", e)
                    storiesLiveData.value = emptyList()
                }
            }
        }

        return storiesLiveData
    }
}


