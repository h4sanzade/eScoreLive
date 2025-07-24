package com.materialdesign.escorelive.presentation.news

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentNewsBinding
import com.materialdesign.escorelive.presentation.adapters.NewsAdapter
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.materialdesign.escorelive.presentation.ui.news.NewsCategory
import com.materialdesign.escorelive.presentation.ui.news.NewsItem
import com.materialdesign.escorelive.presentation.ui.news.NewsViewModel

@AndroidEntryPoint
class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter

    private var isLoadingMore = false
    private var currentCategory = NewsCategory.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryTabs()
        setupSwipeRefresh()
        observeViewModel()
        setupClickListeners()

        Log.d("NewsFragment", "NewsFragment created and initialized")
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter { newsItem ->
            onNewsClick(newsItem)
        }

        binding.newsRecyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)

            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                    // Load more when scrolled to bottom
                    if (!isLoadingMore &&
                        lastVisibleItemPosition >= totalItemCount - 5 &&
                        totalItemCount > 0) {
                        loadMoreNews()
                    }
                }
            })
        }

        Log.d("NewsFragment", "RecyclerView setup completed")
    }

    private fun setupCategoryTabs() {
        // Initially select "All News"
        updateCategorySelection(NewsCategory.ALL)

        binding.categoryAll.setOnClickListener {
            selectCategory(NewsCategory.ALL)
        }

        binding.categoryTransfers.setOnClickListener {
            selectCategory(NewsCategory.TRANSFERS)
        }

        binding.categoryMatches.setOnClickListener {
            selectCategory(NewsCategory.MATCHES)
        }

        binding.categoryInjuries.setOnClickListener {
            selectCategory(NewsCategory.INJURIES)
        }

        Log.d("NewsFragment", "Category tabs setup completed")
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("NewsFragment", "Swipe refresh triggered")
            viewModel.refreshNews()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_color,
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )
    }

    private fun observeViewModel() {
        viewModel.news.observe(viewLifecycleOwner, Observer { newsList ->
            Log.d("NewsFragment", "Received ${newsList.size} news articles")
            newsAdapter.submitList(newsList) {
                // Scroll to top when new category is selected
                if (binding.newsRecyclerView.canScrollVertically(-1)) {
                    binding.newsRecyclerView.scrollToPosition(0)
                }
            }
            updateEmptyState(newsList.isEmpty())
            updateNewsCount(newsList.size)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            Log.d("NewsFragment", "Loading state: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Hide empty state while loading
            if (isLoading) {
                binding.emptyStateLayout.visibility = View.GONE
            }
        })

        viewModel.isRefreshing.observe(viewLifecycleOwner, Observer { isRefreshing ->
            Log.d("NewsFragment", "Refreshing state: $isRefreshing")
            binding.swipeRefreshLayout.isRefreshing = isRefreshing
            isLoadingMore = false
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Log.e("NewsFragment", "Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        viewModel.selectedCategory.observe(viewLifecycleOwner, Observer { category ->
            Log.d("NewsFragment", "Category changed to: $category")
            if (category != currentCategory) {
                currentCategory = category
                updateCategorySelection(category)
            }
        })
    }

    private fun setupClickListeners() {
        // Remove the old refresh button click listener since we're using swipe refresh
    }

    private fun selectCategory(category: NewsCategory) {
        Log.d("NewsFragment", "Selecting category: $category")
        isLoadingMore = false
        viewModel.selectCategory(category)
    }

    private fun updateCategorySelection(category: NewsCategory) {
        // Reset all backgrounds
        binding.categoryAll.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.categoryTransfers.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.categoryMatches.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.categoryInjuries.setBackgroundResource(R.drawable.filter_unselected_bg)

        // Set selected background
        when (category) {
            NewsCategory.ALL -> binding.categoryAll.setBackgroundResource(R.drawable.bottom_line_selected)
            NewsCategory.TRANSFERS -> binding.categoryTransfers.setBackgroundResource(R.drawable.bottom_line_selected)
            NewsCategory.MATCHES -> binding.categoryMatches.setBackgroundResource(R.drawable.bottom_line_selected)
            NewsCategory.INJURIES -> binding.categoryInjuries.setBackgroundResource(R.drawable.bottom_line_selected)
        }
    }

    private fun loadMoreNews() {
        if (isLoadingMore) return

        Log.d("NewsFragment", "Loading more news...")
        isLoadingMore = true
        viewModel.loadMoreNews()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty && viewModel.isLoading.value != true) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.newsRecyclerView.visibility = View.GONE

            // Update empty state text based on category
            val categoryText = when (currentCategory) {
                NewsCategory.TRANSFERS -> "transfer news"
                NewsCategory.MATCHES -> "match reports"
                NewsCategory.INJURIES -> "injury news"
                NewsCategory.ALL -> "news"
            }

            binding.emptyStateTitle.text = "No $categoryText found"
            binding.emptyStateMessage.text = "Pull down to refresh or try a different category"
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.newsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateNewsCount(count: Int) {
        val categoryName = viewModel.getCurrentCategoryString()
        binding.newsDescription.text = "$categoryName • $count articles"
    }

    private fun onNewsClick(newsItem: NewsItem) {
        Log.d("NewsFragment", "News clicked: ${newsItem.title}")
        try {
            val bundle = Bundle().apply {
                putLong("newsId", newsItem.id)
                putString("newsTitle", newsItem.title)
                putString("newsContent", newsItem.content ?: newsItem.summary)
                putString("newsImageUrl", newsItem.imageUrl)
                putString("newsDate", newsItem.publishDate)
                putString("newsSource", newsItem.source)
                putString("newsAuthor", newsItem.author)
                putString("newsUrl", newsItem.url)
                putString("newsCategory", newsItem.category)
            }
            findNavController().navigate(R.id.action_news_to_newsDetail, bundle)
        } catch (e: Exception) {
            Log.e("NewsFragment", "Error navigating to news detail", e)
            Toast.makeText(context, "Opening news: ${newsItem.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("NewsFragment", "Fragment resumed")

        // Refresh news if it's been a while
        val newsCount = viewModel.getNewsCount()
        if (newsCount == 0) {
            viewModel.refreshNews()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("NewsFragment", "Fragment destroyed")
        _binding = null
    }
}