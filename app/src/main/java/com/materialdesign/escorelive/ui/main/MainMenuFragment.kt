package com.materialdesign.escorelive.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.adapter.LiveMatchAdapter
import com.materialdesign.escorelive.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private val viewModel: MainMenuViewModel by viewModels()
    private lateinit var liveMatchesAdapter: LiveMatchAdapter
    private lateinit var liveMatchesRecycler: RecyclerView
    private lateinit var weekRangeText: TextView
    private lateinit var liveHeaderText: TextView

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val weekRangeFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    private var currentWeekOffset = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupCalendar(view)
        observeViewModel()
        setupClickListeners(view)

        liveHeaderText = view.findViewById(R.id.live_header_text)
    }

    private fun setupRecyclerView(view: View) {
        liveMatchesRecycler = view.findViewById(R.id.live_matches_recycler)
        liveMatchesAdapter = LiveMatchAdapter { match ->
            onMatchClick(match)
        }

        liveMatchesRecycler.apply {
            adapter = liveMatchesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
    }

    private fun setupCalendar(view: View) {
        weekRangeText = view.findViewById(R.id.week_range_text)
        updateWeekCalendar()
        setupDayClickListeners(view)

        view.findViewById<View>(R.id.prev_week_btn).setOnClickListener {
            navigateWeek(-7)
        }

        view.findViewById<View>(R.id.next_week_btn).setOnClickListener {
            navigateWeek(7)
        }
    }

    private fun setupDayClickListeners(view: View) {
        val dayLayouts = listOf(
            view.findViewById<View>(R.id.day_1),
            view.findViewById<View>(R.id.day_2),
            view.findViewById<View>(R.id.day_3),
            view.findViewById<View>(R.id.day_4),
            view.findViewById<View>(R.id.day_5),
            view.findViewById<View>(R.id.day_6),
            view.findViewById<View>(R.id.day_7)
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

        weekRangeText.text = "${weekRangeFormat.format(mondayDate)} - ${weekRangeFormat.format(sundayDate)}"

        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val today = Calendar.getInstance()
        var todayIndex = -1

        for (i in 1..7) {
            val dayNameTextView = view?.findViewById<TextView>(resources.getIdentifier("day_${i}_name", "id", requireContext().packageName))
            val dayDateTextView = view?.findViewById<TextView>(resources.getIdentifier("day_${i}_date", "id", requireContext().packageName))

            dayNameTextView?.text = dayNames[i - 1]
            dayDateTextView?.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                dayNameTextView?.text = "Today"
                todayIndex = i - 1
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
        for (i in 1..7) {
            val dayLayout = view?.findViewById<View>(resources.getIdentifier("day_$i", "id", requireContext().packageName))
            if (i - 1 == selectedIndex) {
                dayLayout?.setBackgroundResource(R.drawable.selected_day)
            } else {
                dayLayout?.background = null
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
                    liveHeaderText.text = "Live Now"
                }
                selectedCalendar.before(todayCalendar) -> {
                    val displayDate = displayDateFormat.format(selectedCalendar.time)
                    liveHeaderText.text = "Results - $displayDate"
                }
                selectedCalendar.after(todayCalendar) -> {
                    val displayDate = displayDateFormat.format(selectedCalendar.time)
                    liveHeaderText.text = "Fixtures - $displayDate"
                }
            }
        } catch (e: Exception) {
            liveHeaderText.text = "Matches"
        }
    }

    private fun navigateWeek(days: Int) {
        currentWeekOffset += if (days > 0) 1 else -1
        updateWeekCalendar()
    }

    private fun observeViewModel() {
        viewModel.liveMatches.observe(viewLifecycleOwner, Observer { matches ->
            // Only show live matches when today is selected
            if (viewModel.isSelectedDateToday()) {
                liveMatchesAdapter.submitList(matches)
            }
        })

        viewModel.todayMatches.observe(viewLifecycleOwner, Observer { matches ->
            // Show selected date matches (includes past, present, future)
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

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.see_more_btn).setOnClickListener {
            // Handle see more button click
        }

        view.findViewById<View>(R.id.search_id).setOnClickListener {
            // Handle search click
        }

        view.findViewById<View>(R.id.notification_id).setOnClickListener {
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
}