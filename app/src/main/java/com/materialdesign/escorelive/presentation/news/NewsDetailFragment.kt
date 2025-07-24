package com.materialdesign.escorelive.presentation.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentNewsDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadNewsData()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentScrollView.visibility = View.GONE
    }

    private fun loadNewsData() {
        try {
            val args = arguments
            if (args != null) {
                val newsTitle = args.getString("newsTitle", "")
                val newsContent = args.getString("newsContent", "")
                val newsImageUrl = args.getString("newsImageUrl", "")
                val newsDate = args.getString("newsDate", "")
                val newsSource = args.getString("newsSource", "")
                val newsAuthor = args.getString("newsAuthor")
                val newsUrl = args.getString("newsUrl", "")
                val newsCategory = args.getString("newsCategory", "")

                Log.d("NewsDetailFragment", "Loading news: $newsTitle")

                binding.newsTitle.text = newsTitle

                val displayContent = if (newsContent.isNotEmpty()) {
                    formatNewsContent(newsContent)
                } else {
                    "Full article content is available at the source."
                }
                binding.newsContent.text = displayContent


                loadNewsImage(newsImageUrl)

                binding.publishDate.text = formatPublishDate(newsDate)


                val sourceText = if (!newsAuthor.isNullOrEmpty()) {
                    "By $newsAuthor • $newsSource"
                } else {
                    newsSource
                }
                binding.sourceInfo.text = sourceText

                binding.categoryBadge.text = newsCategory
                setupCategoryBadge(newsCategory)

                binding.readFullArticleBtn.tag = newsUrl
                binding.shareButton.tag = newsUrl

                binding.progressBar.visibility = View.GONE
                binding.contentScrollView.visibility = View.VISIBLE

                Log.d("NewsDetailFragment", "News data loaded successfully")
            } else {
                showError("No news data available")
            }
        } catch (e: Exception) {
            Log.e("NewsDetailFragment", "Error loading news data", e)
            showError("Error loading news details")
        }
    }

    private fun loadNewsImage(imageUrl: String) {
        if (imageUrl.isNotEmpty()) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_news)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            Glide.with(this)
                .load(imageUrl)
                .apply(requestOptions)
                .into(binding.newsImage)

            binding.newsImageCard.visibility = View.VISIBLE
        } else {
            binding.newsImageCard.visibility = View.GONE
        }
    }

    private fun formatNewsContent(content: String): String {
        return content
            .replace(Regex("\\[\\+\\d+ chars\\]"), "") // Remove "[+123 chars]"
            .replace(Regex("….*"), "...") // Clean up truncated content
            .replace("\n\n", "\n")
            .trim()
    }

    private fun formatPublishDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)

            if (date != null) {
                val now = Date()
                val diffInMillis = now.time - date.time
                val diffInHours = diffInMillis / (1000 * 60 * 60)
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

                when {
                    diffInHours < 1 -> "Just now"
                    diffInHours < 24 -> "${diffInHours}h ago"
                    diffInDays < 7 -> "${diffInDays}d ago"
                    else -> {
                        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        displayFormat.format(date)
                    }
                }
            } else {
                dateString
            }
        } catch (e: ParseException) {
            dateString
        }
    }

    private fun setupCategoryBadge(category: String) {
        val (backgroundRes, textColorRes) = when (category.lowercase()) {
            "transfer news", "transfers" -> {
                Pair(R.drawable.indicator_background, R.color.white)
            }
            "match reports", "matches" -> {
                Pair(R.drawable.live_indicator_bg, R.color.white)
            }
            "injury news", "injuries" -> {
                Pair(R.drawable.upcoming_indicator_bg, R.color.white)
            }
            "breaking news" -> {
                Pair(R.drawable.live_indicator_bg, R.color.white)
            }
            else -> {
                Pair(R.drawable.filter_unselected_bg, android.R.color.darker_gray)
            }
        }

        binding.categoryBadge.setBackgroundResource(backgroundRes)
        binding.categoryBadge.setTextColor(resources.getColor(textColorRes, null))
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.readFullArticleBtn.setOnClickListener {
            val url = it.tag as? String
            if (!url.isNullOrEmpty()) {
                openUrlInBrowser(url)
            } else {
                Toast.makeText(context, "Article URL not available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareButton.setOnClickListener {
            val url = it.tag as? String
            val title = binding.newsTitle.text.toString()
            shareNews(title, url)
        }

        binding.bookmarkButton.setOnClickListener {
            Toast.makeText(context, "Bookmark feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("NewsDetailFragment", "Error opening URL", e)
            Toast.makeText(context, "Unable to open article", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareNews(title: String, url: String?) {
        try {
            val shareText = if (!url.isNullOrEmpty()) {
                "$title\n\nRead more: $url"
            } else {
                title
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, title)
            }

            val chooser = Intent.createChooser(shareIntent, "Share news article")
            startActivity(chooser)
        } catch (e: Exception) {
            Log.e("NewsDetailFragment", "Error sharing news", e)
            Toast.makeText(context, "Unable to share article", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentScrollView.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
