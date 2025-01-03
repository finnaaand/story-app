package com.example.mystory.model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagingSource
import com.example.mystory.api.ApiService
import com.example.mystory.data.RegisterResponse
import com.example.mystory.data.login.LoginResponse
import com.example.mystory.data.story.AddStoryResponse
import com.example.mystory.data.story.Story
import com.example.mystory.data.story.StoryDetailResponse
import com.example.mystory.data.story.StoryResponse
import com.example.mystory.paging.StoryPagingSource
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StoryViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: StoryViewModel
    private lateinit var repository: StoryRepository
    private lateinit var apiService: TestApiService

    private val testDispatcher = StandardTestDispatcher()

    private class TestApiService : ApiService {
        private val stories = mutableListOf<Story>()

        init {
            stories.addAll(
                listOf(
                    Story("1", "Story 1", "Description 1", "photo1.jpg", "2024-01-01", -6.8957643, 107.6338462),
                    Story("2", "Story 2", "Description 2", "photo2.jpg", "2024-01-02", -6.8957644, 107.6338463),
                    Story("3", "Story 3", "Description 3", "photo3.jpg", "2024-01-03", -6.8957645, 107.6338464)
                )
            )
        }
        fun getExpectedFirstStory(): Story = stories.first()

        override suspend fun getAllStories(
            token: String,
            page: Int,
            size: Int,
            location: Int?
        ): StoryResponse {
            val startIndex = (page - 1) * size
            val endIndex = minOf(startIndex + size, stories.size)

            val pageStories = if (startIndex >= stories.size) {
                emptyList()
            } else {
                stories.subList(startIndex, endIndex)
            }

            return StoryResponse(
                error = false,
                listStory = pageStories,
                message = "Stories fetched successfully"
            )
        }

        override suspend fun getStoriesWithLocation(token: String): StoryResponse {
            return StoryResponse(
                error = false,
                listStory = stories.filter { it.lat != null && it.lon != null },
                message = "Stories with location fetched successfully"
            )
        }

        fun clearStories() {
            stories.clear()
        }

        override fun register(name: String, email: String, password: String): Call<RegisterResponse> {
            throw NotImplementedError("Not needed for this test")
        }

        override fun login(email: String, password: String): Call<LoginResponse> {
            throw NotImplementedError("Not needed for this test")
        }

        override fun getStoryDetail(token: String, storyId: String): Call<StoryDetailResponse> {
            throw NotImplementedError("Not needed for this test")
        }

        override fun addStory(
            token: String,
            description: RequestBody,
            photo: MultipartBody.Part,
            lat: RequestBody?,
            lon: RequestBody?
        ): Call<AddStoryResponse> {
            throw NotImplementedError("Not needed for this test")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        apiService = TestApiService()
        repository = StoryRepository(apiService)
        viewModel = StoryViewModel(repository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test successful story loading`() = runTest {
        val pagingData = viewModel.getAllStories("Bearer dummy_token").first()

        assertNotNull("PagingData should not be null", pagingData)

        val stories = mutableListOf<Story>()
        val pagingSource = StoryPagingSource(apiService, "Bearer dummy_token")
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        if (loadResult is PagingSource.LoadResult.Page) {
            stories.addAll(loadResult.data)

            assertEquals("Should have 3 stories", 3, stories.size)

            val expectedFirstStory = apiService.getExpectedFirstStory()
            val actualFirstStory = stories.first()

            assertStoryEquals(expectedFirstStory, actualFirstStory)
        }
    }
    private fun assertStoryEquals(expected: Story, actual: Story) {
        assertEquals("Story ID should match", expected.id, actual.id)
        assertEquals("Story name should match", expected.name, actual.name)
        assertEquals("Story description should match", expected.description, actual.description)
        assertEquals("Story photo URL should match", expected.photoUrl, actual.photoUrl)
        assertEquals("Story created at should match", expected.createdAt, actual.createdAt)
        assertEquals("Story latitude should match", expected.lat, actual.lat)
        assertEquals("Story longitude should match", expected.lon, actual.lon)
    }

    @Test
    fun `test empty story data`() = runTest {
        apiService.clearStories()

        val pagingData = viewModel.getAllStories("Bearer dummy_token").first()

        assertNotNull("PagingData should not be null", pagingData)

        val pagingSource = StoryPagingSource(apiService, "Bearer dummy_token")
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        if (loadResult is PagingSource.LoadResult.Page) {
            assertEquals("Should have no stories", 0, loadResult.data.size)
        }
    }
}