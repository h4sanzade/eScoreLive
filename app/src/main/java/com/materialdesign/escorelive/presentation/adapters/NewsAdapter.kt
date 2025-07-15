package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemNewsBinding
import com.materialdesign.escorelive.presentation.ui.news.NewsItem
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class NewsAdapter(
    private val onNewsClick: (NewsItem) -> Unit
) : ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(private val binding: ItemNewsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(newsItem: NewsItem) = with(binding) {
            // Basic info
            newsTitle.text = newsItem.title
            newsSummary.text = newsItem.summary
            newsCategory.text = newsItem.category

            // Load image
            loadNewsImage(newsItem.imageUrl)

            // Set publish date with relative time
            newsDate.text = getRelativeTime(newsItem.publishDate)

            // Calculate and set read time
            readTime.text = calculateReadTime(newsItem.summary)

            // Handle category-specific styling
            setupCategoryStyle(newsItem.category)

            // Handle breaking news
            handleBreakingNews(newsItem)

            // Handle trending indicator
            handleTrendingIndicator(newsItem)

            // Click listener
            root.setOnClickListener { onNewsClick(newsItem) }
        }

        private fun loadNewsImage(imageUrl: String) = with(binding) {
            Glide.with(root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_news)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(newsImage)
        }

        private fun setupCategoryStyle(category: String) = with(binding) {
            when (category.lowercase()) {
                "transfer news", "transfers" -> {
                    newsCategory.setBackgroundResource(R.drawable.indicator_background)
                    newsCategory.setTextColor(ContextCompat.getColor(root.context, R.color.accent_color))
                }
                "match reports", "matches" -> {
                    newsCategory.setBackgroundResource(R.drawable.live_indicator_bg)
                    newsCategory.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
                "injury news", "injuries" -> {
                    newsCategory.setBackgroundResource(R.drawable.upcoming_indicator_bg)
                    newsCategory.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
                "breaking news" -> {
                    newsCategory.setBackgroundResource(R.drawable.live_indicator_bg)
                    newsCategory.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
                else -> {
                    newsCategory.setBackgroundResource(R.drawable.filter_unselected_bg)
                    newsCategory.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
            }
        }

        private fun handleBreakingNews(newsItem: NewsItem) = with(binding) {
            val isBreaking = newsItem.category.contains("breaking", ignoreCase = true) ||
                    newsItem.title.contains("breaking", ignoreCase = true) ||
                    isRecentNews(newsItem.publishDate, 2) // Last 2 hours

            breakingOverlay.visibility = if (isBreaking) View.VISIBLE else View.GONE
        }

        private fun handleTrendingIndicator(newsItem: NewsItem) = with(binding) {
            // Show trending for transfer news or recent popular news
            val isTrending = newsItem.category.contains("transfer", ignoreCase = true) ||
                    newsItem.title.contains("major", ignoreCase = true) ||
                    newsItem.title.contains("exclusive", ignoreCase = true)

            trendingIndicator.visibility = if (isTrending) View.VISIBLE else View.GONE
        }

        private fun getRelativeTime(publishDate: String): String {
            return try {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val now = Date()

                // If the date string contains time info, parse differently
                val date = if (publishDate.contains(":")) {
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).parse(publishDate)
                } else {
                    dateFormat.parse(publishDate)
                } ?: return publishDate

                val diffInMillis = now.time - date.time
                val diffInHours = diffInMillis / (1000 * 60 * 60)
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

                when {
                    diffInHours < 1 -> "Just now"
                    diffInHours < 24 -> "${diffInHours}h ago"
                    diffInDays < 7 -> "${diffInDays}d ago"
                    else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                }
            } catch (e: ParseException) {
                publishDate
            }
        }

        private fun isRecentNews(publishDate: String, hoursThreshold: Int): Boolean {
            return try {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val date = dateFormat.parse(publishDate) ?: return false
                val now = Date()
                val diffInHours = (now.time - date.time) / (1000 * 60 * 60)
                diffInHours <= hoursThreshold
            } catch (e: ParseException) {
                false
            }
        }

        private fun calculateReadTime(content: String): String {
            val wordCount = content.split("\\s+".toRegex()).size
            val wordsPerMinute = 200 // Average reading speed
            val readTimeMinutes = kotlin.math.max(1, wordCount / wordsPerMinute)
            return "${readTimeMinutes} min read"
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<NewsItem>() {
        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem == newItem
        }
    }
}