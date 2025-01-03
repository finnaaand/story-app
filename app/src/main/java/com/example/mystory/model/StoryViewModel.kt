package com.example.mystory.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mystory.data.story.Story
import com.example.mystory.paging.StoryPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class StoryViewModel(private val repository: StoryRepository) : ViewModel() {
    private val _refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllStories(token: String): Flow<PagingData<Story>> {
        return _refreshTrigger
            .flatMapLatest {
                Pager(
                    config = PagingConfig(
                        pageSize = 10,
                        enablePlaceholders = false,
                        initialLoadSize = 10
                    ),
                    pagingSourceFactory = {
                        StoryPagingSource(repository.apiService, token)
                    }
                ).flow
            }
            .cachedIn(viewModelScope)
    }

    fun refreshStories() {
        _refreshTrigger.value += 1
    }

    fun fetchStoriesForWidget(token: String): LiveData<List<Story>> {
        return repository.getStoriesForWidget(token)
    }
}

