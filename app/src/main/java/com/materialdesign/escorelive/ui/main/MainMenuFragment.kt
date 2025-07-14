package com.materialdesign.escorelive.ui.main

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
import com.materialdesign.escorelive.adapter.LiveMatchAdapter
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentMainMenuBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainMenuViewModel by viewModels()
    private lateinit var matchesAdapter: LiveMatchAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val weekRangeFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    private var currentWeekOffset = 0
    private var selectedDayIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()
        setupContentFilters()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        matchesAdapter = LiveMatchAdapter { match ->
            onMatchClick(match)
        }

        binding.matchesRecycler.apply {
            adapter = matchesAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupCalendar() {
        updateWeekCalendar()
        setupDayClickListeners()

        binding.prevWeekBtn.setOnClickListener {
            navigateWeek(-1)
        }

        binding.nextWeekBtn.setOnClickListener {
            navigateWeek(1)
        }
    }

    private fun setupContentFilters() {
        binding.filterUpcoming.setOnClickListener {
            viewModel.setContentFilter(ContentFilter.UPCOMING)
            updateFilterButtons(ContentFilter.UPCOMING)
        }

        binding.filterScore.setOnClickListener {
            viewModel.setContentFilter(ContentFilter.SCORE)
            updateFilterButtons(ContentFilter.SCORE)
        }

        binding.filterFavorites.setOnClickListener {
            viewModel.setContentFilter(ContentFilter.FAVORITES)
            updateFilterButtons(ContentFilter.FAVORITES)
        }
    }

    private fun updateFilterButtons(selectedFilter: ContentFilter) {
        // Reset all buttons
        binding.filterUpcoming.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterScore.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.filterFavorites.setBackgroundResource(R.drawable.filter_unselected_bg)

        // Set selected button
        when (selectedFilter) {
            ContentFilter.UPCOMING -> binding.filterUpcoming.setBackgroundResource(R.drawable.bottom_line_selected)
            ContentFilter.SCORE -> binding.filterScore.setBackgroundResource(R.drawable.bottom_line_selected)
            ContentFilter.FAVORITES -> binding.filterFavorites.setBackgroundResource(R.drawable.bottom_line_selected)
        }
    }

    private fun setupDayClickListeners() {
        val dayLayouts = listOf(
            binding.day1, binding.day2, binding.day3, binding.day4,
            binding.day5, binding.day6, binding.day7
        )

        dayLayouts.forEachIndexed { index, dayLayout ->
            dayLayout.setOnClickListener {
                selectedDayIndex = index
                val selectedDate = getDateForDayIndex(index)
                viewModel.selectDate(selectedDate)
                updateSelectedDay(index)
            }
        }
    }

    private fun updateWeekCalendar() {
        val today = Calendar.getInstance()
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.WEEK_OF_YEAR, currentWeekOffset)

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> -6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> -1
            Calendar.WEDNESDAY -> -2
            Calendar.THURSDAY -> -3
            Calendar.FRIDAY -> -4
            Calendar.SATURDAY -> -5
            else -> 0
        }
        calendar.add(Calendar.DAY_OF_MONTH, daysFromMonday)

        val mondayDate = calendar.time
        val sundayCalendar = calendar.clone() as Calendar
        sundayCalendar.add(Calendar.DAY_OF_MONTH, 6)
        val sundayDate = sundayCalendar.time

        binding.weekRangeText.text = "${weekRangeFormat.format(mondayDate)} - ${weekRangeFormat.format(sundayDate)}"

        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayNameViews = listOf(
            binding.day1Name, binding.day2Name, binding.day3Name, binding.day4Name,
            binding.day5Name, binding.day6Name, binding.day7Name
        )
        val dayDateViews = listOf(
            binding.day1Date, binding.day2Date, binding.day3Date, binding.day4Date,
            binding.day5Date, binding.day6Date, binding.day7Date
        )

        var todayIndex = -1
        calendar.time = mondayDate

        for (i in 0..6) {
            dayNameViews[i].text = dayNames[i]
            dayDateViews[i].text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                dayNameViews[i].text = "Today"
                todayIndex = i
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (todayIndex != -1 && selectedDayIndex == -1) {
            selectedDayIndex = todayIndex
        } else if (selectedDayIndex == -1) {
            selectedDayIndex = 0
        }

        updateSelectedDay(selectedDayIndex)
        val selectedDate = getDateForDayIndex(selectedDayIndex)
        viewModel.selectDate(selectedDate)
    }

    private fun getDateForDayIndex(dayIndex: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeekOffset)

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> -6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> -1
            Calendar.WEDNESDAY -> -2
            Calendar.THURSDAY -> -3
            Calendar.FRIDAY -> -4
            Calendar.SATURDAY -> -5
            else -> 0
        }
        calendar.add(Calendar.DAY_OF_MONTH, daysFromMonday + dayIndex)

        return dateFormat.format(calendar.time)
    }

    private fun updateSelectedDay(selectedIndex: Int) {
        val dayLayouts = listOf(
            binding.day1, binding.day2, binding.day3, binding.day4,
            binding.day5, binding.day6, binding.day7
        )

        dayLayouts.forEachIndexed { index, dayLayout ->
            if (index == selectedIndex) {
                dayLayout.setBackgroundResource(R.drawable.selected_day)
            } else {
                dayLayout.background = null
            }
        }
    }

    private fun navigateWeek(weekOffset: Int) {
        currentWeekOffset += weekOffset
        selectedDayIndex = -1
        updateWeekCalendar()
    }

    private fun observeViewModel() {
        viewModel.matches.observe(viewLifecycleOwner, Observer { matches ->
            matchesAdapter.submitList(matches)
            updateEmptyState(matches.isEmpty())
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Handle loading state if needed
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })

        viewModel.selectedContentFilter.observe(viewLifecycleOwner, Observer { filter ->
            updateFilterButtons(filter)
        })
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.matchesRecycler.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.matchesRecycler.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.searchId.setOnClickListener {
            val action = MainMenuFragmentDirections.actionMainMenuToSearch()
            findNavController().navigate(action)
        }

        binding.notificationId.setOnClickListener {
            // Handle notification click
        }
    }

    private fun onMatchClick(match: LiveMatch) {
        val action = MainMenuFragmentDirections.actionMainMenuToMatchDetail(match.id)
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}