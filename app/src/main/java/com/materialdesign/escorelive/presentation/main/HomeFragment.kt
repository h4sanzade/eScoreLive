package com.materialdesign.escorelive.presentation.ui.main

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
import com.materialdesign.escorelive.presentation.adapters.LiveMatchAdapter
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentMainMenuBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var liveMatchesAdapter: LiveMatchAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val weekRangeFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    private var currentWeekOffset = 0
    private var selectedDayIndex = -1
    private var currentTab = MatchTab.UPCOMING

    enum class MatchTab {
        UPCOMING, SCORE, FAVORITES
    }

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
        setupTabs()
        setupBottomNavigation()
        observeViewModel()
        setupClickListeners()

        loadDataForSelectedDate()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.homeFragment

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Already here, do nothing
                    true
                }
                R.id.competitionFragment -> {
                    try {
                        findNavController().navigate(R.id.action_home_to_competition)
                    } catch (e: Exception) {
                        // Handle navigation error silently
                    }
                    true
                }
                R.id.newsFragment -> {
                    try {
                        findNavController().navigate(R.id.action_home_to_news)
                    } catch (e: Exception) {
                        // Handle navigation error silently
                    }
                    true
                }
                R.id.accountFragment -> {
                    try {
                        findNavController().navigate(R.id.action_home_to_account)
                    } catch (e: Exception) {
                        // Handle navigation error silently
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        liveMatchesAdapter = LiveMatchAdapter { match ->
            onMatchClick(match)
        }

        binding.liveMatchesRecycler.apply {
            adapter = liveMatchesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            visibility = View.VISIBLE
        }
    }

    private fun setupTabs() {
        updateTabSelection(MatchTab.UPCOMING)

        binding.upcomingTab.setOnClickListener {
            currentTab = MatchTab.UPCOMING
            updateTabSelection(MatchTab.UPCOMING)
            loadDataForSelectedDate()
        }

        binding.scoreTab.setOnClickListener {
            currentTab = MatchTab.SCORE
            updateTabSelection(MatchTab.SCORE)
            loadDataForSelectedDate()
        }

        binding.favoritesTab.setOnClickListener {
            currentTab = MatchTab.FAVORITES
            updateTabSelection(MatchTab.FAVORITES)
            loadFavoriteMatches()
        }
    }

    private fun updateTabSelection(selectedTab: MatchTab) {
        binding.upcomingTab.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.scoreTab.setBackgroundResource(R.drawable.filter_unselected_bg)
        binding.favoritesTab.setBackgroundResource(R.drawable.filter_unselected_bg)

        when (selectedTab) {
            MatchTab.UPCOMING -> binding.upcomingTab.setBackgroundResource(R.drawable.bottom_line_selected)
            MatchTab.SCORE -> binding.scoreTab.setBackgroundResource(R.drawable.bottom_line_selected)
            MatchTab.FAVORITES -> binding.favoritesTab.setBackgroundResource(R.drawable.bottom_line_selected)
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
                updateHeaderBasedOnSelectedDate(selectedDate)

                if (currentTab != MatchTab.FAVORITES) {
                    loadDataForSelectedDate()
                }
            }
        }
    }

    private fun loadDataForSelectedDate() {
        val selectedDate = viewModel.selectedDate.value ?: dateFormat.format(Date())

        when (currentTab) {
            MatchTab.UPCOMING -> {
                viewModel.loadUpcomingMatches(selectedDate)
                updateLiveHeaderText("Upcoming Matches")
            }
            MatchTab.SCORE -> {
                viewModel.loadLiveAndFinishedMatches(selectedDate)
                updateLiveHeaderText("Live & Results")
            }
            MatchTab.FAVORITES -> {
                loadFavoriteMatches()
                updateLiveHeaderText("Favorite Teams")
            }
        }
    }

    private fun loadFavoriteMatches() {
        viewModel.loadFavoriteTeamMatches()
        updateLiveHeaderText("Favorite Teams")
    }

    private fun updateLiveHeaderText(text: String) {
        binding.liveHeaderText.text = text
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
        updateHeaderBasedOnSelectedDate(selectedDate)
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

    private fun updateHeaderBasedOnSelectedDate(selectedDate: String) {
        val today = dateFormat.format(Date())

        try {
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
                selectedDate == today -> {
                    when (currentTab) {
                        MatchTab.UPCOMING -> updateLiveHeaderText("Today's Upcoming")
                        MatchTab.SCORE -> updateLiveHeaderText("Live Now")
                        MatchTab.FAVORITES -> updateLiveHeaderText("Favorite Teams")
                    }
                }
                selectedCalendar.before(todayCalendar) -> {
                    val displayDate = displayDateFormat.format(selectedCalendar.time)
                    when (currentTab) {
                        MatchTab.FAVORITES -> updateLiveHeaderText("Favorite Teams")
                        else -> updateLiveHeaderText("Results - $displayDate")
                    }
                }
                selectedCalendar.after(todayCalendar) -> {
                    val displayDate = displayDateFormat.format(selectedCalendar.time)
                    when (currentTab) {
                        MatchTab.FAVORITES -> updateLiveHeaderText("Favorite Teams")
                        else -> updateLiveHeaderText("Fixtures - $displayDate")
                    }
                }
            }
        } catch (e: Exception) {
            updateLiveHeaderText("Matches")
        }
    }

    private fun navigateWeek(weekOffset: Int) {
        currentWeekOffset += weekOffset
        selectedDayIndex = -1
        updateWeekCalendar()

        if (currentTab != MatchTab.FAVORITES) {
            loadDataForSelectedDate()
        }
    }

    private fun observeViewModel() {
        viewModel.liveMatches.observe(viewLifecycleOwner, Observer { matches ->
            if (currentTab == MatchTab.SCORE) {
                liveMatchesAdapter.submitList(matches)
            }
        })

        viewModel.todayMatches.observe(viewLifecycleOwner, Observer { matches ->
            when (currentTab) {
                MatchTab.UPCOMING -> {
                    val upcomingMatches = matches.filter { it.isUpcoming }
                    liveMatchesAdapter.submitList(upcomingMatches)
                }
                MatchTab.SCORE -> {
                    val liveAndFinished = matches.filter { it.isLive || it.isFinished }
                    liveMatchesAdapter.submitList(liveAndFinished)
                }
                MatchTab.FAVORITES -> {
                    // Handled separately
                }
            }
        })

        viewModel.favoriteMatches.observe(viewLifecycleOwner, Observer { matches ->
            if (currentTab == MatchTab.FAVORITES) {
                liveMatchesAdapter.submitList(matches)
            }
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
            when (currentTab) {
                MatchTab.FAVORITES -> {
                    try {
                        val bundle = Bundle().apply {
                            putString("selectedDate", "FAVORITES_MODE")
                        }
                        findNavController().navigate(R.id.action_home_to_allMatches, bundle)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening favorite team matches...", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    val selectedDate = viewModel.selectedDate.value ?: dateFormat.format(Date())
                    try {
                        val bundle = Bundle().apply {
                            putString("selectedDate", selectedDate)
                        }
                        findNavController().navigate(R.id.action_home_to_allMatches, bundle)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening matches...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.searchId.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_home_to_teamSearch)
            } catch (e: Exception) {
                Toast.makeText(context, "Opening team search...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.notificationId.setOnClickListener {
            Toast.makeText(context, "Notifications clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onMatchClick(match: Match) {
        try {
            val bundle = Bundle().apply {
                putLong("matchId", match.id)
            }
            findNavController().navigate(R.id.action_home_to_matchDetail, bundle)
        } catch (e: Exception) {
            Toast.makeText(context, "Opening match details...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDataForSelectedDate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}