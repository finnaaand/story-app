package com.example.mystory.widget


import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mystory.R
import com.example.mystory.api.ApiConfig.apiService
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.session.SessionRepository
import com.example.mystory.model.StoryRepository
import com.example.mystory.model.StoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryWidgetProvider : AppWidgetProvider() {
    private lateinit var sessionRepository: SessionRepository
    private lateinit var storyRepository: StoryRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        sessionRepository = SessionRepository(SessionManager(context))
        storyRepository = StoryRepository(apiService)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_story)

        CoroutineScope(Dispatchers.IO).launch {
            val token = sessionRepository.getUserToken()?.let { "Bearer $it" }

            withContext(Dispatchers.Main) {
                if (token != null) {
                    val viewModel = StoryViewModel(storyRepository)
                    val storiesLiveData = viewModel.fetchStoriesForWidget(token)
                    storiesLiveData.observeForever { stories ->
                        if (stories.isNotEmpty()) {
                            views.setTextViewText(R.id.story_title, stories[0].name)
                            loadImageIntoWidget(context, stories[0].photoUrl, views, appWidgetManager, appWidgetId)
                        } else {
                            views.setTextViewText(R.id.story_title, "No Stories Available")
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        storiesLiveData.removeObserver { }
                    }
                } else {
                    views.setTextViewText(R.id.story_title, "Error: Token not found.")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    private fun loadImageIntoWidget(
        context: Context,
        imageUrl: String,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    views.setImageViewBitmap(R.id.story_image, resource)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    views.setImageViewResource(R.id.story_image, R.drawable.ic_placeholder)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            })
    }
}
