package com.example.mystory.data.story

data class StoryDetailResponse(
    val error: Boolean,
    val message: String,
    val story: Story
)