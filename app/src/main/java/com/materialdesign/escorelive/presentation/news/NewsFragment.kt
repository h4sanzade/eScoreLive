package com.materialdesign.escorelive.presentation.ui.news

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
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentNewsBinding
import com.materialdesign.escorelive.presentation.adapters.NewsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter

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
        setupUI()
        observeViewModel()
        setupClickListeners()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter { newsItem ->
            onNewsClick(newsItem)
        }

        binding.newsRecyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupUI() {
        // Setup UI components
        binding.newsTitle.text = "Latest News"
        binding.newsDescription.text = "Stay updated with the latest football news and transfer updates"
    }

    private fun observeViewModel() {
        // Observe news data
        viewModel.news.observe(viewLifecycleOwner, Observer { newsList ->
            newsAdapter.submitList(newsList)
            updateEmptyState(newsList.isEmpty())
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = isLoading
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })
    }

    private fun setupClickListeners() {
        binding.refreshButton.setOnClickListener {
            viewModel.refreshNews()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshNews()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_color,
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            // Could show an empty state view here
            Toast.makeText(context, "No news available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onNewsClick(newsItem: NewsItem) {
        // Navigate to news detail when implemented
        Toast.makeText(context, "Opening: ${newsItem.title}", Toast.LENGTH_SHORT).show()

        val action = NewsFragmentDirections.actionNewsToNewsDetail(newsItem.id)
         findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}