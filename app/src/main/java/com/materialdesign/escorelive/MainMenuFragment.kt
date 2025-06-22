package com.materialdesign.escorelive

import LiveMatch
import LiveMatchAdapter
import MainMenuViewModel
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
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private val viewModel: MainMenuViewModel by viewModels()
    private lateinit var liveMatchesAdapter: LiveMatchAdapter
    private lateinit var liveMatchesRecycler: RecyclerView

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

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
        // Setup calendar with current week
        updateWeekCalendar()

        // Setup day click listeners
        setupDayClickListeners(view)

        // Setup week navigation
        view.findViewById<View>(R.id.prev_week_btn).setOnClickListener {
            // Navigate to previous week
            navigateWeek(-7)
        }

        view.findViewById<View>(R.id.next_week_btn).setOnClickListener {
            // Navigate to next week
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
            }
        }
    }

    private fun updateWeekCalendar() {
        val calendar = Calendar.getInstance()

        // Start from Monday of current week
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (i in 1..7) {
            val dayNameTextView = view?.findViewById<TextView>(resources.getIdentifier("day_${i}_name", "id", requireContext().packageName))
            val dayDateTextView = view?.findViewById<TextView>(resources.getIdentifier("day_${i}_date", "id", requireContext().packageName))

            dayNameTextView?.text = dayNames[i - 1]
            dayDateTextView?.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            // Check if it's today
            val today = Calendar.getInstance()
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                dayNameTextView?.text = "Today"
                updateSelectedDay(i - 1)
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun getDateForDayIndex(dayIndex: Int): String {
        val calendar = Calendar.getInstance()
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

    private fun navigateWeek(days: Int) {
        // Implementation for week navigation
        updateWeekCalendar()
    }

    private fun observeViewModel() {
        viewModel.liveMatches.observe(viewLifecycleOwner, Observer { matches ->
            liveMatchesAdapter.submitList(matches)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Show/hide loading indicator
            // You can add a progress bar to your layout
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
        }

        view.findViewById<View>(R.id.search_id).setOnClickListener {
        }

        view.findViewById<View>(R.id.notification_id).setOnClickListener {
        }
    }

    private fun onMatchClick(match: LiveMatch) {
        Toast.makeText(context, "Match clicked: ${match.homeTeam.name} vs ${match.awayTeam.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}