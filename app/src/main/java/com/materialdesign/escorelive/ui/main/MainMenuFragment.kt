package com.materialdesign.escorelive.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
    private lateinit var liveMatchesAdapter: LiveMatchAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val weekRangeFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    private var currentWeekOffset = 0

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
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        liveMatchesAdapter = LiveMatchAdapter { match ->
            onMatchClick(match)
        }

        binding.liveMatchesRecycler.apply {
            adapter = liveMatchesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
    }

    private fun setupCalendar() {
        updateWeekCalendar()
        setupDayClickListeners()

        binding.prevWeekBtn.setOnClickListener {
            navigateWeek(-7)
        }

        binding.nextWeekBtn.setOnClickListener {
            navigateWeek(7)
        }
    }

    private fun setupDayClickListeners() {
        val dayLayouts = listOf(
            binding.day1,
            binding.day2,
            binding.day3,
            binding.day4,
            binding.day5,
            binding.day6,
            binding.day7
        )

        dayLayouts.forEachIndexed { index, dayLayout ->
            dayLayout.setOnClickListener {
                val selectedDate = getDateForDayIndex(index)
                viewModel.selectDate(selectedDate)
                updateSelectedDay(index)
                updateHeaderBasedOnSelectedDate(selectedDate)
            }
        }
    }

    private fun updateWeekCalendar() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeekOffset)

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

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

        val today = Calendar.getInstance()
        var todayIndex = -1

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

        if (todayIndex != -1) {
            updateSelectedDay(todayIndex)
            val todayDate = getDateForDayIndex(todayIndex)
            viewModel.selectDate(todayDate)
            updateHeaderBasedOnSelectedDate(todayDate)
        } else {
            updateSelectedDay(0)
            val firstDayDate = getDateForDayIndex(0)
            viewModel.selectDate(firstDayDate)
            updateHeaderBasedOnSelectedDate(firstDayDate)
        }
    }

    private fun getDateForDayIndex(dayIndex: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeekOffset)

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday + dayIndex)

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

    private fun updateHeaderBasedOnSelectedDate(selectedDate: String) {
        val today = dateFormat.format(Date())
        val selectedCalendar = Calendar.getInstance()
        val todayCalendar = Calendar.getInstance()

        try {
            selectedCalendar.time = dateFormat.parse(selectedDate) ?: Date()

            when {
                selectedDate == today -> {
                    binding.liveHeaderText.text = "Live Now"
                }
                selectedCalendar.before(todayCalendar) -> {
                    val displayDate = displayDateFormat.format(selectedCalendar.time)
                    binding.liveHeaderText.text = "Results - $displayDate"
                }
                selectedCalendar.after(todayCalendar) -> {
                    val displayDate = displayDateFormat.format(selectedCalendar.time)
                    binding.liveHeaderText.text = "Fixtures - $displayDate"
                }

            }
        } catch (e: Exception) {
            binding.liveHeaderText.text = "Matches"
        }
    }

    private fun navigateWeek(days: Int) {
        currentWeekOffset += if (days > 0) 1 else -1
        updateWeekCalendar()
    }

    private fun observeViewModel() {
        viewModel.liveMatches.observe(viewLifecycleOwner, Observer { matches ->
            if (viewModel.isSelectedDateToday()) {
                liveMatchesAdapter.submitList(matches)
            }
        })

        viewModel.todayMatches.observe(viewLifecycleOwner, Observer { matches ->
            liveMatchesAdapter.submitList(matches)
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
    }

    private fun setupClickListeners() {
        binding.seeMoreBtn.setOnClickListener {
            // Handle see more button click
        }

        binding.searchId.setOnClickListener {
            // Handle search click
        }

        binding.notificationId.setOnClickListener {
            // Handle notification click
        }
    }

    private fun onMatchClick(match: LiveMatch) {
        val matchInfo = when {
            match.isFinished -> "${match.homeTeam.name} ${match.homeScore} - ${match.awayScore} ${match.awayTeam.name} (Final)"
            match.isLive -> "${match.homeTeam.name} vs ${match.awayTeam.name} (Live)"
            else -> "${match.homeTeam.name} vs ${match.awayTeam.name}"
        }
        Toast.makeText(context, matchInfo, Toast.LENGTH_SHORT).show()
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