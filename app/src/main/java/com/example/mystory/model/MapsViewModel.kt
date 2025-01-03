package com.example.mystory.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.story.Story
import kotlinx.coroutines.launch

class MapsViewModel(
    private val storyRepository: StoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _storiesWithLocation = MutableLiveData<List<Story>>()
    val storiesWithLocation: LiveData<List<Story>> get() = _storiesWithLocation
    private val _errorMessage = MutableLiveData<String>()

    fun fetchStoriesWithLocation() {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                if (token.isNotEmpty()) {
                    val response = storyRepository.getStoriesWithLocation(token)
                    val stories = response.listStory.filter { it.lat != null && it.lon != null }

                    _storiesWithLocation.postValue(stories)
                } else {
                    _errorMessage.postValue("User not logged in. Please log in again.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to load map data: ${e.localizedMessage}")
            }
        }
    }

}


