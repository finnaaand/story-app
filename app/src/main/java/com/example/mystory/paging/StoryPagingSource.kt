package com.example.mystory.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.mystory.api.ApiService
import com.example.mystory.data.story.Story


class StoryPagingSource(
    private val apiService: ApiService,
    private val token: String
) : PagingSource<Int, Story>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Story> {
        return try {
            val page = params.key ?: INITIAL_PAGE_INDEX
            val response = apiService.getAllStories(
                token = token,
                page = page,
                size = params.loadSize
            )
            val stories = response.listStory
            Log.d("StoryPagingSource", "Loaded ${stories.size} stories for page $page")

            LoadResult.Page(
                data = stories,
                prevKey = if (page == INITIAL_PAGE_INDEX) null else page - 1,
                nextKey = if (stories.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            Log.e("StoryPagingSource", "Error loading stories", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Story>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    companion object {
        const val INITIAL_PAGE_INDEX = 1
    }
}
