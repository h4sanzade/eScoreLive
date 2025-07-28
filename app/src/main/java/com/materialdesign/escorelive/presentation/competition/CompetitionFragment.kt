// CompetitionFragment.kt - Final version with SwipeRefreshLayout
package com.materialdesign.escorelive.presentation.competition

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionTab
import com.materialdesign.escorelive.databinding.FragmentCompetitionBinding
import com.materialdesign.escorelive.presentation.adapters.CompetitionAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CompetitionFragment : Fragment() {

    private var _binding: FragmentCompetitionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CompetitionViewModel by viewModels()
    private lateinit var competitionAdapter: CompetitionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompetitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabLayout()
        setupSearchBar()
        setupSwipeRefresh()
        setupBottomNavigation()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        competitionAdapter = CompetitionAdapter(
            onCompetitionClick = { competition ->
                onCompetitionClick(competition)
            },
            onFavoriteClick = { competition ->
                viewModel.toggleFavorite(competition)
            }
        )

        binding.swipeRefreshLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.competitions_recycler_view)?.apply {
            adapter = competitionAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedTab = when (tab?.position) {
                    0 -> CompetitionTab.TOP
                    1 -> CompetitionTab.REGION
                    2 -> CompetitionTab.FAVORITES
                    else -> CompetitionTab.TOP
                }
                viewModel.selectTab(selectedTab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set default selected tab
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            viewModel.onSearchQueryChanged(query)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        // Customize swipe refresh colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_color,
            R.color.accent_color,
            R.color.accent_color
        )
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.competitionFragment

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    try {
                        findNavController().navigate(R.id.action_competition_to_home)
                    } catch (e: Exception) {
                        showError("Navigation error")
                    }
                    true
                }
                R.id.competitionFragment -> {
                    // Already here
                    true
                }
                R.id.newsFragment -> {
                    try {
                        findNavController().navigate(R.id.action_competition_to_news)
                    } catch (e: Exception) {
                        showError("Navigation error")
                    }
                    true
                }
                R.id.accountFragment -> {
                    try {
                        findNavController().navigate(R.id.action_competition_to_account)
                    } catch (e: Exception) {
                        showError("Navigation error")
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: com.materialdesign.escorelive.data.remote.dto.CompetitionUiState) {
        // Update loading state
        val progressBar = binding.swipeRefreshLayout.findViewById<ProgressBar>(R.id.progress_bar)
        progressBar?.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Update swipe refresh state
        if (binding.swipeRefreshLayout.isRefreshing && !state.isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Update competitions list
        competitionAdapter.submitList(state.filteredCompetitions)

        // Update empty state
        val isEmpty = state.filteredCompetitions.isEmpty() && !state.isLoading
        val emptyStateLayout = binding.swipeRefreshLayout.findViewById<LinearLayout>(R.id.empty_state_layout)
        val recyclerView = binding.swipeRefreshLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.competitions_recycler_view)

        emptyStateLayout?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (isEmpty) View.GONE else View.VISIBLE

        // Handle errors
        state.error?.let { error ->
            showError(error)
            viewModel.clearError()
        }

        // Update search query in UI if needed
        if (binding.searchEditText.text.toString() != state.searchQuery) {
            binding.searchEditText.setText(state.searchQuery)
            binding.searchEditText.setSelection(state.searchQuery.length)
        }

        // Update empty state message based on selected tab
        updateEmptyStateMessage(state.selectedTab)
    }

    private fun updateEmptyStateMessage(selectedTab: CompetitionTab) {
        val emptyStateLayout = binding.swipeRefreshLayout.findViewById<LinearLayout>(R.id.empty_state_layout)
        val titleView = emptyStateLayout?.getChildAt(1) as? android.widget.TextView
        val messageView = emptyStateLayout?.getChildAt(2) as? android.widget.TextView

        when (selectedTab) {
            CompetitionTab.TOP -> {
                titleView?.text = "No top competitions found"
                messageView?.text = "Try refreshing or check your connection"
            }
            CompetitionTab.REGION -> {
                titleView?.text = "No regional competitions found"
                messageView?.text = "Try searching for a specific region or country"
            }
            CompetitionTab.FAVORITES -> {
                titleView?.text = "No favorite competitions"
                messageView?.text = "Add competitions to favorites by tapping the heart icon"
            }
        }
    }

    private fun onCompetitionClick(competition: Competition) {
        // Show selected competition
        Toast.makeText(
            context,
            "Selected: ${competition.name} (${competition.country})",
            Toast.LENGTH_SHORT
        ).show()

        // TODO: Navigate to competition details
        // Example navigation:
        // try {
        //     val action = CompetitionFragmentDirections
        //         .actionCompetitionToCompetitionDetail(competition.id, competition.name)
        //     findNavController().navigate(action)
        // } catch (e: Exception) {
        //     showError("Failed to open competition details")
        // }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the fragment
        if (::competitionAdapter.isInitialized) {
            competitionAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}