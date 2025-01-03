package com.example.mystory.helper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mystory.R

class StoryLoadStateAdapter(private val retry: () -> Unit) : LoadStateAdapter<StoryLoadStateAdapter.LoadStateViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.load_state_footer, parent, false)
        return LoadStateViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    inner class LoadStateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        private val retryButton: Button = view.findViewById(R.id.retryButton)

        fun bind(loadState: LoadState) {
            progressBar.visibility = if (loadState is LoadState.Loading) View.VISIBLE else View.GONE
            retryButton.visibility = if (loadState is LoadState.Error) View.VISIBLE else View.GONE

            retryButton.setOnClickListener { retry() }
        }
    }
}
