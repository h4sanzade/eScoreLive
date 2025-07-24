package com.materialdesign.escorelive.data.remote.mappers

import com.materialdesign.escorelive.data.remote.NewsArticle
import com.materialdesign.escorelive.presentation.ui.news.NewsItem
import com.materialdesign.escorelive.presentation.ui.news.NewsCategory
import java.text.SimpleDateFormat
import java.util.*

object NewsMapper {

    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private val outputDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    fun mapToNewsItem(article: NewsArticle, category: NewsCategory): NewsItem {
        val articleId = generateArticleId(article.title, article.publishedAt)

        val formattedDate = try {
            inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputDateFormat.parse(article.publishedAt)
            date?.let { outputDateFormat.format(it) } ?: article.publishedAt
        } catch (e: Exception) {
            article.publishedAt
        }

        val categoryName = determineCategoryName(article, category)
        val imageUrl = article.urlToImage ?: getDefaultImageForCategory(category)

        return NewsItem(
            id = articleId,
            title = cleanTitle(article.title),
            summary = cleanSummary(article.description ?: article.content ?: ""),
            imageUrl = imageUrl,
            publishDate = formattedDate,
            category = categoryName,
            url = article.url,
            source = article.source.name,
            author = article.author,
            content = article.content
        )
    }

    private fun generateArticleId(title: String, publishedAt: String): Long {
        return (title + publishedAt).hashCode().toLong()
    }

    private fun cleanTitle(title: String): String {
        // Remove common suffixes from news titles
        return title
            .replace(Regex(" - .*$"), "") // Remove " - Source Name" suffixes
            .replace(Regex(" \\| .*$"), "") // Remove " | Source Name" suffixes
            .trim()
    }

    private fun cleanSummary(description: String): String {
        return description
            .replace(Regex("\\[\\+\\d+ chars\\]"), "") // Remove "[+123 chars]" from content
            .replace(Regex("….*"), "...") // Clean up truncated content
            .trim()
            .take(200) // Limit summary length
    }

    private fun determineCategoryName(article: NewsArticle, requestedCategory: NewsCategory): String {
        val title = article.title.lowercase()
        val description = (article.description ?: "").lowercase()
        val content = title + " " + description

        return when {
            // Transfer related keywords
            content.contains("transfer") ||
                    content.contains("signing") ||
                    content.contains("contract") ||
                    content.contains("deal") ||
                    content.contains("joins") ||
                    content.contains("signs") -> "Transfer News"

            // Injury related keywords
            content.contains("injury") ||
                    content.contains("injured") ||
                    content.contains("fitness") ||
                    content.contains("recovery") ||
                    content.contains("surgery") ||
                    content.contains("sidelined") -> "Injury News"

            // Match related keywords
            content.contains("match") ||
                    content.contains("game") ||
                    content.contains("result") ||
                    content.contains("score") ||
                    content.contains("victory") ||
                    content.contains("defeat") ||
                    content.contains("draw") ||
                    content.contains("vs") ||
                    content.contains("against") -> "Match Reports"

            // Breaking news indicators
            content.contains("breaking") ||
                    content.contains("urgent") ||
                    content.contains("exclusive") -> "Breaking News"

            // Default based on requested category
            else -> when (requestedCategory) {
                NewsCategory.TRANSFERS -> "Transfer News"
                NewsCategory.MATCHES -> "Match Reports"
                NewsCategory.INJURIES -> "Injury News"
                NewsCategory.ALL -> "Football News"
            }
        }
    }

    private fun getDefaultImageForCategory(category: NewsCategory): String {
        return when (category) {
            NewsCategory.TRANSFERS -> "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=400"
            NewsCategory.MATCHES -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=400"
            NewsCategory.INJURIES -> "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400"
            NewsCategory.ALL -> "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=400"
        }
    }
}