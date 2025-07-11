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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentAllMatchesBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AllMatchesFragment : Fragment() {

    private var _binding: FragmentAllMatchesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllMatchesViewModel by viewModels()
    private lateinit var allMatchesAdapter: AllMatchesAdapter

    private val args: AllMatchesFragmentArgs by navArgs()

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

        val selectedDate = args.selectedDate.ifEmpty { dateFormat.format(Date()) }
        val displayType = determineDisplayType(selectedDate)

        updateUIForDisplayType(displayType, selectedDate)

        viewModel.loadMatchesForDate(selectedDate, displayType)
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
        // Update header title
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
        }
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
        viewModel.matches.observe(viewLifecycleOwner, Observer { matches ->
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
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.matchesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun onMatchClick(match: LiveMatch) {
        val action = AllMatchesFragmentDirections.actionAllMatchesToMatchDetail(match.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class DisplayType {
    PAST, TODAY, FUTURE
}