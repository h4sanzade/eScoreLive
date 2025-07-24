package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemNewsBinding
import com.materialdesign.escorelive.presentation.ui.news.NewsItem
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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
            newsTitle.text = newsItem.title
            newsSummary.text = newsItem.summary
            newsCategory.text = newsItem.category

            loadNewsImage(newsItem.imageUrl)

            newsDate.text = getRelativeTime(newsItem.publishDate)

            readTime.text = calculateReadTime(newsItem.summary)

            setupCategoryStyle(newsItem.category)

            handleBreakingNews(newsItem)

            handleTrendingIndicator(newsItem)

            if (!newsItem.source.isNullOrEmpty()) {
                newsSummary.text = "${newsItem.summary}\n\nSource: ${newsItem.source}"
            }

            root.setOnClickListener {
                addClickAnimation()
                onNewsClick(newsItem)
            }
        }

        private fun loadNewsImage(imageUrl: String) = with(binding) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_news)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            Glide.with(root.context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(newsImage)
        }

        private fun setupCategoryStyle(category: String) = with(binding) {
            val categoryLower = category.lowercase()
            val (backgroundRes, textColorRes) = when {
                categoryLower.contains("transfer") -> {
                    Pair(R.drawable.indicator_background, R.color.white)
                }
                categoryLower.contains("match") -> {
                    Pair(R.drawable.live_indicator_bg, R.color.white)
                }
                categoryLower.contains("injur") -> {
                    Pair(R.drawable.upcoming_indicator_bg, R.color.white)
                }
                categoryLower.contains("breaking") -> {
                    Pair(R.drawable.live_indicator_bg, R.color.white)
                }
                else -> {
                    Pair(R.drawable.filter_unselected_bg, android.R.color.darker_gray)
                }
            }

            newsCategory.setBackgroundResource(backgroundRes)
            newsCategory.setTextColor(ContextCompat.getColor(root.context, textColorRes))

            if (categoryLower.contains("breaking")) {
                newsCategory.animate()
                    .alpha(0.7f)
                    .setDuration(1000)
                    .withEndAction {
                        newsCategory.animate()
                            .alpha(1.0f)
                            .setDuration(1000)
                            .start()
                    }
                    .start()
            }
        }

        private fun handleBreakingNews(newsItem: NewsItem) = with(binding) {
            val categoryLower = newsItem.category.lowercase()
            val titleLower = newsItem.title.lowercase()

            val isBreaking = categoryLower.contains("breaking") ||
                    titleLower.contains("breaking") ||
                    isRecentNews(newsItem.publishDate, 2) // Last 2 hours

            breakingOverlay.visibility = if (isBreaking) View.VISIBLE else View.GONE

            if (isBreaking) {
                breakingOverlay.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(500)
                    .withEndAction {
                        breakingOverlay.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(500)
                            .start()
                    }
                    .start()
            }
        }

        private fun handleTrendingIndicator(newsItem: NewsItem) = with(binding) {
            val categoryLower = newsItem.category.lowercase()
            val titleLower = newsItem.title.lowercase()

            val isTrending = categoryLower.contains("transfer") ||
                    titleLower.contains("major") ||
                    titleLower.contains("exclusive") ||
                    titleLower.contains("confirmed") ||
                    titleLower.contains("deal") ||
                    titleLower.contains("signing")

            trendingIndicator.visibility = if (isTrending) View.VISIBLE else View.GONE

            if (isTrending) {
                trendingIndicator.animate()
                    .rotationBy(360f)
                    .setDuration(2000)
                    .start()
            }
        }

        private fun getRelativeTime(publishDate: String): String {
            return try {
                val possiblePatterns = listOf(
                    "dd MMM yyyy HH:mm",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "dd MMM yyyy"
                )

                var date: Date? = null

                for (pattern in possiblePatterns) {
                    try {
                        val format = SimpleDateFormat(pattern, Locale.getDefault())
                        if (pattern.contains("'Z'")) {
                            format.timeZone = TimeZone.getTimeZone("UTC")
                        }
                        date = format.parse(publishDate)
                        if (date != null) break
                    } catch (e: ParseException) {
                        continue
                    }
                }

                if (date == null) {
                    return publishDate
                }

                val now = Date()
                val diffInMillis = now.time - date.time
                val diffInMinutes = diffInMillis / (1000 * 60)
                val diffInHours = diffInMillis / (1000 * 60 * 60)
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

                when {
                    diffInMinutes < 1 -> "Just now"
                    diffInMinutes < 60 -> "${diffInMinutes}m ago"
                    diffInHours < 24 -> "${diffInHours}h ago"
                    diffInDays < 7 -> "${diffInDays}d ago"
                    else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                publishDate
            }
        }


        private fun isRecentNews(publishDate: String, hoursThreshold: Int): Boolean {
            return try {
                val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
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

        private fun addClickAnimation() = with(binding) {
            root.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    root.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
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