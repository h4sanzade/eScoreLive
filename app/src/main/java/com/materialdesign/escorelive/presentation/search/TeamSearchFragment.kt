package com.materialdesign.escorelive.presentation.search

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
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentTeamSearchBinding
import com.materialdesign.escorelive.presentation.adapters.TeamSearchAdapter
import com.materialdesign.escorelive.presentation.adapters.StandingsAdapter
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomsheet.BottomSheetDialog

@AndroidEntryPoint
class TeamSearchFragment : Fragment() {

    private var _binding: FragmentTeamSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TeamSearchViewModel by viewModels()
    private lateinit var teamSearchAdapter: TeamSearchAdapter
    private var standingsBottomSheet: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
        setupClickListeners()

        // Show initial state
        showInitialState()
    }

    private fun setupRecyclerView() {
        teamSearchAdapter = TeamSearchAdapter(
            onTeamClick = { teamSearchResult ->
                // Show team details or matches
                Toast.makeText(context, "Selected: ${teamSearchResult.team.name}", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to team details/matches
                // val action = TeamSearchFragmentDirections.actionTeamSearchToTeamDetail(teamSearchResult.team.id)
                // findNavController().navigate(action)
            },
            onFavoriteClick = { teamSearchResult ->
                handleFavoriteClick(teamSearchResult)
            },
            onStandingsClick = { teamSearchResult ->
                showStandingsBottomSheet(teamSearchResult)
            },
            isTeamFavorite = { teamId ->
                viewModel.isTeamFavorite(teamId)
            }
        )

        binding.searchResultsRecyclerView.apply {
            adapter = teamSearchAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun handleFavoriteClick(teamSearchResult: TeamSearchResult) {
        val team = teamSearchResult.team

        if (viewModel.isTeamFavorite(team.id)) {
            viewModel.removeFromFavorites(team.id)
            Toast.makeText(context, "${team.name} removed from favorites ❤️", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.addToFavorites(team.id)
            Toast.makeText(context, "${team.name} added to favorites 💚", Toast.LENGTH_SHORT).show()
        }

        // Update favorites counter
        updateFavoritesCounter()
        // Refresh adapter to update heart icon
        teamSearchAdapter.notifyDataSetChanged()
    }

    private fun showStandingsBottomSheet(teamSearchResult: TeamSearchResult) {
        // Load standings for the team's league
        viewModel.loadTeamStandings(teamSearchResult)

        // Show loading toast
        Toast.makeText(context, "Loading standings for ${teamSearchResult.leagueName}...", Toast.LENGTH_SHORT).show()

        // Create and show bottom sheet
        createStandingsBottomSheet(teamSearchResult)
    }

    private fun createStandingsBottomSheet(teamSearchResult: TeamSearchResult) {
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_standings, null)

        standingsBottomSheet = BottomSheetDialog(requireContext()).apply {
            setContentView(bottomSheetView)

            // Setup standings RecyclerView
            val standingsRecyclerView = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.standings_recycler_view)
            val standingsAdapter = StandingsAdapter()

            standingsRecyclerView.apply {
                adapter = standingsAdapter
                layoutManager = LinearLayoutManager(context)
            }

            // Set title
            val titleView = bottomSheetView.findViewById<android.widget.TextView>(R.id.standings_title)
            titleView.text = "${teamSearchResult.leagueName} Standings"

            // Set season
            val seasonView = bottomSheetView.findViewById<android.widget.TextView>(R.id.standings_season)
            seasonView.text = "Season ${teamSearchResult.season}"

            // Close button
            val closeButton = bottomSheetView.findViewById<android.widget.ImageView>(R.id.close_button)
            closeButton.setOnClickListener {
                dismiss()
            }

            // Show the bottom sheet immediately
            show()
        }

        // Observe standings data separately - not inside the bottom sheet
        observeStandingsForBottomSheet(teamSearchResult, standingsBottomSheet!!)
    }

    private fun observeStandingsForBottomSheet(
        teamSearchResult: TeamSearchResult,
        bottomSheet: BottomSheetDialog
    ) {
        viewModel.selectedTeamStandings.observe(viewLifecycleOwner) { standings ->
            if (standings.isNotEmpty() && bottomSheet.isShowing) {
                val bottomSheetView = bottomSheet.findViewById<View>(android.R.id.content)
                val standingsRecyclerView = bottomSheetView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.standings_recycler_view)
                val standingsAdapter = standingsRecyclerView?.adapter as? StandingsAdapter

                standingsAdapter?.submitList(standings)

                // Highlight selected team
                val teamPosition = viewModel.getTeamPositionInStandings(teamSearchResult.team.id)
                teamPosition?.let { position ->
                    val teamPositionView = bottomSheetView?.findViewById<android.widget.TextView>(R.id.team_position)
                    teamPositionView?.text = "${teamSearchResult.team.name} is in position ${position.rank}"
                    teamPositionView?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchQuery ->
                    if (searchQuery.isNotEmpty()) {
                        viewModel.searchTeams(searchQuery)
                        binding.clearSearchBtn.visibility = View.VISIBLE
                        hideInitialState()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return when {
                    newText.isNullOrEmpty() -> {
                        viewModel.clearSearch()
                        binding.clearSearchBtn.visibility = View.GONE
                        showInitialState()
                        true
                    }
                    newText.length >= 2 -> {
                        viewModel.searchTeams(newText)
                        binding.clearSearchBtn.visibility = View.VISIBLE
                        hideInitialState()
                        true
                    }
                    else -> {
                        hideInitialState()
                        true
                    }
                }
            }
        })

        // Set focus to search view when fragment opens
        binding.searchView.requestFocus()
        binding.searchView.isIconified = false
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner, Observer { teams ->
            teamSearchAdapter.submitList(teams)

            when {
                !viewModel.hasSearchQuery() -> {
                    showInitialState()
                }
                teams.isEmpty() -> {
                    showEmptyState()
                }
                else -> {
                    showSearchResults()
                }
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                hideAllStates()
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        viewModel.selectedTeamStandings.observe(viewLifecycleOwner, Observer { standings ->
            // This observer will be used by the bottom sheet
            // The actual handling is done in createStandingsBottomSheet method
        })
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.clearSearchBtn.setOnClickListener {
            clearSearch()
        }

        // Update favorites counter initially
        updateFavoritesCounter()
    }

    private fun updateFavoritesCounter() {
        val favoritesCount = viewModel.getFavoriteTeamsCount()
        binding.favoritesCountText.text = "$favoritesCount favorite teams"
        binding.favoritesCountText.visibility = if (favoritesCount > 0) View.VISIBLE else View.GONE
    }

    private fun clearSearch() {
        binding.searchView.setQuery("", false)
        viewModel.clearSearch()
        binding.clearSearchBtn.visibility = View.GONE
        showInitialState()
    }

    private fun showInitialState() {
        binding.initialStateLayout.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        updateFavoritesCounter()
    }

    private fun showEmptyState() {
        binding.initialStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.searchResultsRecyclerView.visibility = View.GONE
    }

    private fun showSearchResults() {
        binding.initialStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
    }

    private fun hideInitialState() {
        binding.initialStateLayout.visibility = View.GONE
    }

    private fun hideAllStates() {
        binding.initialStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite status when returning to fragment
        teamSearchAdapter.notifyDataSetChanged()
        updateFavoritesCounter()
    }

    override fun onPause() {
        super.onPause()
        // Dismiss bottom sheet when leaving fragment
        standingsBottomSheet?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        // Clear any ongoing searches
        viewModel.clearSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up resources
        standingsBottomSheet?.dismiss()
        standingsBottomSheet = null
        _binding = null
    }
}