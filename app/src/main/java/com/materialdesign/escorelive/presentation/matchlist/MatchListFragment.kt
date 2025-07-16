package com.materialdesign.escorelive.presentation.ui.matchlist

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
import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentAllMatchesBinding
import com.materialdesign.escorelive.presentation.adapters.MatchListAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

enum class DisplayType {
    PAST, TODAY, FUTURE, FAVORITES
}

enum class MatchFilter {
    ALL, LIVE, FINISHED, UPCOMING
}

@AndroidEntryPoint
class MatchListFragment : Fragment() {

    private var _binding: FragmentAllMatchesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MatchListViewModel by viewModels()
    private lateinit var matchListAdapter: MatchListAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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

        // Check if this is favorites mode
        val selectedDate = arguments?.getString("selectedDate") ?: dateFormat.format(Date())
        val isFavoritesMode = selectedDate == "FAVORITES_MODE"

        if (isFavoritesMode) {
            setupFavoritesMode()
        } else {
            val displayType = determineDisplayType(selectedDate)
            updateUIForDisplayType(displayType, selectedDate)
            viewModel.loadMatchesForDate(selectedDate, displayType)
        }
    }

    private fun setupFavoritesMode() {
        binding.headerTitle.text = "Favorite Teams Matches"

        // Show all filter buttons for favorites
        binding.filterScrollView.visibility = View.VISIBLE
        binding.filterAll.visibility = View.VISIBLE
        binding.filterLive.visibility = View.VISIBLE
        binding.filterFinished.visibility = View.VISIBLE
        binding.filterUpcoming.visibility = View.VISIBLE

        // Set filter texts for favorites context
        binding.filterAll.text = "All Matches"
        binding.filterLive.text = "Live Now"
        binding.filterFinished.text = "Finished"
        binding.filterUpcoming.text = "Upcoming"

        // Load favorite team matches
        viewModel.loadFavoriteTeamMatches()

        // Show info about favorites
        showFavoritesInfo()
    }

    private fun showFavoritesInfo() {
        val favoritesCount = viewModel.getFavoriteTeamsCount()
        if (favoritesCount == 0) {
            // Show empty state for no favorites
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.matchesRecyclerView.visibility = View.GONE

            // Update empty state text for favorites
            binding.emptyStateLayout.findViewById<android.widget.TextView>(R.id.empty_state_title)?.text = "No Favorite Teams"
            binding.emptyStateLayout.findViewById<android.widget.TextView>(R.id.empty_state_message)?.text = "Add teams to favorites to see their matches here"
        } else {
            Toast.makeText(context, "Showing matches for $favoritesCount favorite teams", Toast.LENGTH_SHORT).show()
        }
    }

    private fun determineDisplayType(selectedDate: String): DisplayType {
        val today = dateFormat.format(Date())

        return try {
            val selectedCalendar = Calendar.getInstance()
            val todayCalendar = Calendar.getInstance()

            selectedCalendar.time = dateFormat.parse(selectedDate) ?: Date()
            todayCalendar.time = dateFormat.parse(today) ?: Date()

            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)

            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            todayCalendar.set(Calendar.SECOND, 0)
            todayCalendar.set(Calendar.MILLISECOND, 0)

            when {
                selectedCalendar.before(todayCalendar) -> DisplayType.PAST
                selectedCalendar.after(todayCalendar) -> DisplayType.FUTURE
                else -> DisplayType.TODAY
            }
        } catch (e: Exception) {
            DisplayType.TODAY
        }
    }

    private fun updateUIForDisplayType(displayType: DisplayType, selectedDate: String) {
        val formattedDate = try {
            val date = dateFormat.parse(selectedDate)
            date?.let { displayDateFormat.format(it) } ?: "Matches"
        } catch (e: Exception) {
            "Matches"
        }

        binding.headerTitle.text = when (displayType) {
            DisplayType.PAST -> "Results - $formattedDate"
            DisplayType.TODAY -> "Today's Matches"
            DisplayType.FUTURE -> "Fixtures - $formattedDate"
            DisplayType.FAVORITES -> "Favorite Teams Matches"
        }

        when (displayType) {
            DisplayType.PAST -> {
                binding.filterScrollView.visibility = View.GONE
            }
            DisplayType.TODAY -> {
                binding.filterScrollView.visibility = View.VISIBLE
                binding.filterAll.visibility = View.VISIBLE
                binding.filterLive.visibility = View.VISIBLE
                binding.filterFinished.visibility = View.VISIBLE
                binding.filterUpcoming.visibility = View.VISIBLE
            }
            DisplayType.FUTURE -> {
                binding.filterScrollView.visibility = View.GONE
            }
            DisplayType.FAVORITES -> {
                binding.filterScrollView.visibility = View.VISIBLE
                binding.filterAll.visibility = View.VISIBLE
                binding.filterLive.visibility = View.VISIBLE
                binding.filterFinished.visibility = View.VISIBLE
                binding.filterUpcoming.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        matchListAdapter = MatchListAdapter { match ->
            onMatchClick(match)
        }

        binding.matchesRecyclerView.apply {
            adapter = matchListAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshMatches()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_color,
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )
    }

    private fun observeViewModel() {
        viewModel.matches.observe(viewLifecycleOwner, Observer { matches ->
            matchListAdapter.submitList(matches)
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
            findNavController().popBackStack()
        }

        binding.filterAll.setOnClickListener {
            viewModel.setFilter(MatchFilter.ALL)
        }

        binding.filterLive.setOnClickListener {
            viewModel.setFilter(MatchFilter.LIVE)
        }

        binding.filterFinished.setOnClickListener {
            viewModel.setFilter(MatchFilter.FINISHED)
        }

        binding.filterUpcoming.setOnClickListener {
            viewModel.setFilter(MatchFilter.UPCOMING)
        }
    }

    private fun updateFilterButtons(selectedFilter: MatchFilter) {
        binding.filterAll.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterLive.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterFinished.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterUpcoming.setBackgroundResource(R.drawable.filter_unselected_bg)

        when (selectedFilter) {
            MatchFilter.ALL -> binding.filterAll.setBackgroundResource(R.drawable.bottom_line_selected)
            MatchFilter.LIVE -> binding.filterLive.setBackgroundResource(R.drawable.bottom_line_selected)
            MatchFilter.FINISHED -> binding.filterFinished.setBackgroundResource(R.drawable.bottom_line_selected)
            MatchFilter.UPCOMING -> binding.filterUpcoming.setBackgroundResource(R.drawable.bottom_line_selected)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.matchesRecyclerView.visibility = View.GONE

            // Update empty state message based on context
            if (viewModel.isFavoritesModeActive()) {
                if (viewModel.getFavoriteTeamsCount() == 0) {
                    // No favorite teams at all - show custom message
                    showCustomEmptyState("No Favorite Teams", "Add teams to favorites to see their matches here")
                } else {
                    // Have favorite teams but no matches for current filter
                    val filterName = when (viewModel.selectedFilter.value) {
                        MatchFilter.LIVE -> "live"
                        MatchFilter.FINISHED -> "finished"
                        MatchFilter.UPCOMING -> "upcoming"
                        else -> ""
                    }
                    showCustomEmptyState("No $filterName matches", "Try a different filter or check back later")
                }
            } else {
                // Regular date-based empty state
                showCustomEmptyState("No matches found", "Pull down to refresh")
            }
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.matchesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showCustomEmptyState(title: String, message: String) {
        // Create custom empty state views if they don't exist in layout
        // Since we can't be sure about the exact layout structure, we'll use a simpler approach

        // Try to find existing TextView elements or use Toast as fallback
        try {
            val titleView = binding.emptyStateLayout.findViewById<android.widget.TextView>(R.id.empty_state_title)
            val messageView = binding.emptyStateLayout.findViewById<android.widget.TextView>(R.id.empty_state_message)

            titleView?.text = title
            messageView?.text = message
        } catch (e: Exception) {
            // If layout doesn't have these specific IDs, show as toast
            Toast.makeText(context, "$title: $message", Toast.LENGTH_LONG).show()
        }
    }

    private fun onMatchClick(match: Match) {
        try {
            val bundle = Bundle().apply {
                putLong("matchId", match.id)
            }
            findNavController().navigate(R.id.action_allMatches_to_matchDetail, bundle)
        } catch (e: Exception) {
            Toast.makeText(context, "Opening match details...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}