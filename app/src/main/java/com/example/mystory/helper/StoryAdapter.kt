package com.example.mystory.helper

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mystory.R
import com.example.mystory.data.story.Story

class StoryAdapter(
    private val clickListener: (Story, ImageView) -> Unit
) : PagingDataAdapter<Story, StoryAdapter.StoryViewHolder>(StoryDiffCallback()) {

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.tv_item_name)
        private val photo: ImageView = view.findViewById(R.id.iv_item_photo)

        fun bind(story: Story?) {
            story?.let { storyData ->
                name.text = storyData.name
                Glide.with(itemView.context)
                    .load(storyData.photoUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(photo)

                itemView.setOnClickListener { clickListener(storyData, photo) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = getItem(position)
        Log.d("StoryAdapter", "Binding story at position $position: $story")
        holder.bind(story)
    }
}

class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
    override fun areItemsTheSame(oldItem: Story, newItem: Story): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Story, newItem: Story): Boolean {
        return oldItem == newItem
    }
}


