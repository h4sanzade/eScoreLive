package com.materialdesign.escorelive.ui.allmatchs

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
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentAllMatchesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllMatchesFragment : Fragment() {

    private var _binding: FragmentAllMatchesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllMatchesViewModel by viewModels()
    private lateinit var allMatchesAdapter: AllMatchesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllMatchesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
        setupSwipeRefresh()

        // Load initial data
        viewModel.loadAllMatches()
    }

    private fun setupRecyclerView() {
        allMatchesAdapter = AllMatchesAdapter { match ->
            onMatchClick(match)
        }

        binding.matchesRecyclerView.apply {
            adapter = allMatchesAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshMatches()
        }

        // Customize swipe refresh colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_color,
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )
    }

    private fun observeViewModel() {
        viewModel.allMatches.observe(viewLifecycleOwner, Observer { matches ->
            allMatchesAdapter.submitList(matches)
            updateEmptyState(matches.isEmpty())
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        viewModel.selectedFilter.observe(viewLifecycleOwner, Observer { filter ->
            updateFilterButtons(filter)
        })
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            // Navigation Component ile geri git
            findNavController().popBackStack()
        }

        binding.filterAll.setOnClickListener {
            viewModel.setFilter(MatchFilter.ALL)
        }

        binding.filterLive.setOnClickListener {
            viewModel.setFilter(MatchFilter.LIVE)
        }

        binding.filterToday.setOnClickListener {
            viewModel.setFilter(MatchFilter.TODAY)
        }

        binding.filterFinished.setOnClickListener {
            viewModel.setFilter(MatchFilter.FINISHED)
        }

        binding.filterUpcoming.setOnClickListener {
            viewModel.setFilter(MatchFilter.UPCOMING)
        }
    }

    private fun updateFilterButtons(selectedFilter: MatchFilter) {
        // Reset all buttons
        binding.filterAll.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterLive.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterToday.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterFinished.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterUpcoming.setBackgroundResource(R.drawable.filter_unselected_bg)

        // Set selected button
        when (selectedFilter) {
            MatchFilter.ALL -> binding.filterAll.setBackgroundResource(R.drawable.filter_selected_bg)
            MatchFilter.LIVE -> binding.filterLive.setBackgroundResource(R.drawable.filter_selected_bg)
            MatchFilter.TODAY -> binding.filterToday.setBackgroundResource(R.drawable.filter_selected_bg)
            MatchFilter.FINISHED -> binding.filterFinished.setBackgroundResource(R.drawable.filter_selected_bg)
            MatchFilter.UPCOMING -> binding.filterUpcoming.setBackgroundResource(R.drawable.filter_selected_bg)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.matchesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.matchesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun onMatchClick(match: LiveMatch) {
        // Navigation Component ile Match Detail Fragment'e git
        val action = AllMatchesFragmentDirections.actionAllMatchesToMatchDetail(match.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}