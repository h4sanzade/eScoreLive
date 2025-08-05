package com.materialdesign.escorelive.presentation.competition

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.bumptech.glide.Glide
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionTab
import com.materialdesign.escorelive.databinding.FragmentCompetitionBinding
import com.materialdesign.escorelive.presentation.adapters.CompetitionAdapter
import com.materialdesign.escorelive.presentation.adapters.RegionCompetitionAdapter
import com.materialdesign.escorelive.presentation.adapters.StandingsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CompetitionFragment : Fragment() {

    private var _binding: FragmentCompetitionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CompetitionViewModel by viewModels()
    private lateinit var competitionAdapter: CompetitionAdapter
    private lateinit var regionAdapter: RegionCompetitionAdapter

    private var standingsBottomSheetDialog: BottomSheetDialog? = null

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

        showLoadingMessage()
    }

    private fun setupRecyclerView() {
        competitionAdapter = CompetitionAdapter(
            onCompetitionClick = { competition ->
                onCompetitionClick(competition)
            },
            onFavoriteClick = { competition ->
                viewModel.toggleFavorite(competition)
                showFavoriteMessage(competition)
            },
            onStandingsClick = { competition ->
                showStandingsBottomSheet(competition)
            }
        )

        regionAdapter = RegionCompetitionAdapter(
            onCompetitionClick = { competition ->
                onCompetitionClick(competition)
            },
            onFavoriteClick = { competition ->
                viewModel.toggleFavorite(competition)
                showFavoriteMessage(competition)
            },
            onCountryHeaderClick = { country ->
                Toast.makeText(context, "Showing leagues for $country", Toast.LENGTH_SHORT).show()
            },
            onStandingsClick = { competition ->
                showStandingsBottomSheet(competition)
            }
        )

        val recyclerView = binding.swipeRefreshLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.competitions_recycler_view)
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = competitionAdapter
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
                updateTabDescription(selectedTab)
                switchAdapter(selectedTab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }

    private fun switchAdapter(tab: CompetitionTab) {
        val recyclerView = binding.swipeRefreshLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.competitions_recycler_view)

        when (tab) {
            CompetitionTab.REGION -> {
                recyclerView?.adapter = regionAdapter
            }
            else -> {
                recyclerView?.adapter = competitionAdapter
            }
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            viewModel.onSearchQueryChanged(query)
        }

        updateSearchHint(CompetitionTab.TOP)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            showRefreshMessage()
            viewModel.refresh()
        }

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
                R.id.competitionFragment -> true
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.regionalCompetitions.collect { regionalData ->
                    regionAdapter.submitRegionalData(regionalData)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.standingsData.collect { standings ->
                    updateStandingsBottomSheet(standings)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.standingsLoading.collect { isLoading ->
                    updateStandingsLoadingState(isLoading)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.standingsError.collect { error ->
                    error?.let {
                        showError(it)
                        viewModel.clearStandingsError()
                    }
                }
            }
        }
    }

    private fun updateUI(state: com.materialdesign.escorelive.data.remote.dto.CompetitionUiState) {
        val progressBar = binding.swipeRefreshLayout.findViewById<ProgressBar>(R.id.progress_bar)
        progressBar?.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        if (binding.swipeRefreshLayout.isRefreshing && !state.isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
        }

        when (state.selectedTab) {
            CompetitionTab.REGION -> {
            }
            else -> {
                competitionAdapter.submitList(state.filteredCompetitions) {
                    binding.swipeRefreshLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.competitions_recycler_view)?.scrollToPosition(0)
                }
            }
        }

        val isEmpty = when (state.selectedTab) {
            CompetitionTab.REGION -> regionAdapter.itemCount == 0 && !state.isLoading
            else -> state.filteredCompetitions.isEmpty() && !state.isLoading
        }

        val emptyStateLayout = binding.swipeRefreshLayout.findViewById<LinearLayout>(R.id.empty_state_layout)
        val recyclerView = binding.swipeRefreshLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.competitions_recycler_view)

        emptyStateLayout?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (isEmpty) View.GONE else View.VISIBLE

        state.error?.let { error ->
            showError(error)
            viewModel.clearError()
        }

        if (binding.searchEditText.text.toString() != state.searchQuery) {
            binding.searchEditText.setText(state.searchQuery)
            binding.searchEditText.setSelection(state.searchQuery.length)
        }

        updateEmptyStateMessage(state.selectedTab)
        updateHeaderWithCount(state, regionAdapter.itemCount)
    }

    private fun updateEmptyStateMessage(selectedTab: CompetitionTab) {
        val emptyStateLayout = binding.swipeRefreshLayout.findViewById<LinearLayout>(R.id.empty_state_layout)
        val titleView = emptyStateLayout?.getChildAt(1) as? android.widget.TextView
        val messageView = emptyStateLayout?.getChildAt(2) as? android.widget.TextView

        when (selectedTab) {
            CompetitionTab.TOP -> {
                titleView?.text = "No top competitions found"
                messageView?.text = "Top leagues and tournaments will appear here"
            }
            CompetitionTab.REGION -> {
                titleView?.text = "No competitions by region"
                messageView?.text = "Leagues grouped by country will appear here"
            }
            CompetitionTab.FAVORITES -> {
                titleView?.text = "No favorite competitions"
                messageView?.text = "Add competitions to favorites by tapping the heart icon"
            }
        }
    }

    private fun updateHeaderWithCount(state: com.materialdesign.escorelive.data.remote.dto.CompetitionUiState, regionCount: Int) {
        val count = when (state.selectedTab) {
            CompetitionTab.REGION -> regionCount
            else -> state.filteredCompetitions.size
        }

        val tabName = when (state.selectedTab) {
            CompetitionTab.TOP -> "Top Competitions"
            CompetitionTab.REGION -> "By Region"
            CompetitionTab.FAVORITES -> "Favorites"
        }

        if (count > 0) {
            binding.headerTitle.text = "$tabName ($count)"
        } else {
            binding.headerTitle.text = tabName
        }
    }

    private fun updateTabDescription(selectedTab: CompetitionTab) {
        updateSearchHint(selectedTab)
    }

    private fun updateSearchHint(tab: CompetitionTab) {
        binding.searchEditText.hint = when (tab) {
            CompetitionTab.TOP -> "Search top competitions..."
            CompetitionTab.REGION -> "Search by country or league name..."
            CompetitionTab.FAVORITES -> "Search your favorites..."
        }
    }

    private fun showStandingsBottomSheet(competition: Competition) {
        Toast.makeText(context, "Loading ${competition.name} standings...", Toast.LENGTH_SHORT).show()

        createStandingsBottomSheet(competition)

        viewModel.loadCompetitionStandings(competition)
    }

    private fun createStandingsBottomSheet(competition: Competition) {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_standings, null)

        standingsBottomSheetDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(bottomSheetView)

            val titleView = bottomSheetView.findViewById<TextView>(R.id.standings_title)
            val seasonView = bottomSheetView.findViewById<TextView>(R.id.standings_season)
            val leagueLogoView = bottomSheetView.findViewById<ImageView>(R.id.league_logo)
            val closeButton = bottomSheetView.findViewById<ImageView>(R.id.close_button)
            val recyclerView = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.standings_recycler_view)

            titleView.text = "${competition.name} Standings"
            seasonView.text = "Season ${competition.season ?: "2024"}"

            Glide.with(this@CompetitionFragment)
                .load(competition.logoUrl)
                .placeholder(R.drawable.ic_competition)
                .into(leagueLogoView)

            val standingsAdapter = StandingsAdapter()
            recyclerView.apply {
                adapter = standingsAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }

            closeButton.setOnClickListener {
                dismiss()
            }

            show()
        }
    }

    private fun updateStandingsBottomSheet(standings: List<com.materialdesign.escorelive.data.remote.TeamStanding>) {
        standingsBottomSheetDialog?.let { dialog ->
            if (dialog.isShowing) {
                val bottomSheetView = dialog.findViewById<View>(android.R.id.content)
                val recyclerView = bottomSheetView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.standings_recycler_view)
                val adapter = recyclerView?.adapter as? StandingsAdapter

                adapter?.submitList(standings)

                val progressBar = bottomSheetView?.findViewById<ProgressBar>(R.id.standings_progress_bar)
                val emptyState = bottomSheetView?.findViewById<LinearLayout>(R.id.standings_empty_state)

                progressBar?.visibility = View.GONE

                if (standings.isEmpty()) {
                    emptyState?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.GONE
                } else {
                    emptyState?.visibility = View.GONE
                    recyclerView?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateStandingsLoadingState(isLoading: Boolean) {
        standingsBottomSheetDialog?.let { dialog ->
            if (dialog.isShowing) {
                val bottomSheetView = dialog.findViewById<View>(android.R.id.content)
                val progressBar = bottomSheetView?.findViewById<ProgressBar>(R.id.standings_progress_bar)
                val recyclerView = bottomSheetView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.standings_recycler_view)
                val emptyState = bottomSheetView?.findViewById<LinearLayout>(R.id.standings_empty_state)

                if (isLoading) {
                    progressBar?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.GONE
                    emptyState?.visibility = View.GONE
                } else {
                    progressBar?.visibility = View.GONE
                }
            }
        }
    }

    private fun onCompetitionClick(competition: Competition) {
        val message = when {
            competition.currentSeason -> "${competition.name} (Current Season)"
            !competition.season.isNullOrEmpty() -> "${competition.name} (${competition.season})"
            else -> competition.name
        }

        Toast.makeText(context, "$message - ${competition.country}", Toast.LENGTH_SHORT).show()
    }

    private fun showFavoriteMessage(competition: Competition) {
        val message = if (competition.isFavorite) {
            "${competition.name} removed from favorites"
        } else {
            "${competition.name} added to favorites"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoadingMessage() {
        Toast.makeText(context, "Loading competitions from Football API...", Toast.LENGTH_SHORT).show()
    }

    private fun showRefreshMessage() {
        Toast.makeText(context, "Refreshing competitions...", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (::competitionAdapter.isInitialized) {
            competitionAdapter.notifyDataSetChanged()
        }
        if (::regionAdapter.isInitialized) {
            regionAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        standingsBottomSheetDialog?.dismiss()
        standingsBottomSheetDialog = null
        viewModel.clearStandingsData()
        _binding = null
    }
}