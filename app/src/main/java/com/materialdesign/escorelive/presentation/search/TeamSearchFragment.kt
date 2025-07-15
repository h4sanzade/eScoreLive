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
import com.google.android.material.chip.Chip
import android.util.Log

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
        Log.d("TeamSearchFragment", "onCreateView called")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TeamSearchFragment", "onViewCreated called")

        setupRecyclerView()
        setupSearchView()
        setupSuggestions()
        observeViewModel()
        setupClickListeners()

        // Show initial state
        showInitialState()

        // Test the search functionality
        Log.d("TeamSearchFragment", "Setup complete")
    }

    private fun setupRecyclerView() {
        Log.d("TeamSearchFragment", "setupRecyclerView called")

        teamSearchAdapter = TeamSearchAdapter(
            onTeamClick = { teamSearchResult ->
                Log.d("TeamSearchFragment", "Team clicked: ${teamSearchResult.team.name}")
                Toast.makeText(
                    context,
                    "Selected: ${teamSearchResult.team.name} (${teamSearchResult.leagueName})",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.addToSearchHistory(teamSearchResult.team.name)
            },
            onFavoriteClick = { teamSearchResult ->
                Log.d("TeamSearchFragment", "Favorite clicked: ${teamSearchResult.team.name}")
                handleFavoriteClick(teamSearchResult)
            },
            onStandingsClick = { teamSearchResult ->
                Log.d("TeamSearchFragment", "Standings clicked: ${teamSearchResult.team.name}")
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

    private fun setupSearchView() {
        Log.d("TeamSearchFragment", "setupSearchView called")

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("TeamSearchFragment", "onQueryTextSubmit: '$query'")
                query?.let { searchQuery ->
                    if (searchQuery.isNotEmpty()) {
                        viewModel.searchTeams(searchQuery)
                        viewModel.addToSearchHistory(searchQuery)
                        binding.clearSearchBtn.visibility = View.VISIBLE
                        hideInitialState()
                        hideSuggestions()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("TeamSearchFragment", "onQueryTextChange: '$newText'")
                return when {
                    newText.isNullOrEmpty() -> {
                        Log.d("TeamSearchFragment", "Query empty, clearing search")
                        viewModel.clearSearch()
                        binding.clearSearchBtn.visibility = View.GONE
                        showInitialState()
                        hideSuggestions()
                        true
                    }
                    newText.length == 1 -> {
                        Log.d("TeamSearchFragment", "First character, getting suggestions")
                        viewModel.getSuggestions(newText)
                        hideInitialState()
                        showSuggestions()
                        true
                    }
                    newText.length >= 2 -> {
                        Log.d("TeamSearchFragment", "2+ characters, searching and getting suggestions")
                        viewModel.searchTeams(newText)
                        viewModel.getSuggestions(newText)
                        binding.clearSearchBtn.visibility = View.VISIBLE
                        hideInitialState()
                        showSuggestions()
                        true
                    }
                    else -> {
                        hideInitialState()
                        true
                    }
                }
            }
        })

        // SearchView'a focus ver
        binding.searchView.requestFocus()
        binding.searchView.isIconified = false
    }

    private fun setupSuggestions() {
        Log.d("TeamSearchFragment", "setupSuggestions called")
        binding.suggestionsContainer.visibility = View.GONE
    }

    private fun observeViewModel() {
        Log.d("TeamSearchFragment", "observeViewModel called")

        viewModel.searchResults.observe(viewLifecycleOwner, Observer { teams ->
            Log.d("TeamSearchFragment", "Search results received: ${teams.size} teams")
            teamSearchAdapter.submitList(teams)

            when {
                !viewModel.hasSearchQuery() -> {
                    Log.d("TeamSearchFragment", "No search query, showing initial state")
                    showInitialState()
                }
                teams.isEmpty() -> {
                    Log.d("TeamSearchFragment", "Empty results, showing empty state")
                    showEmptyState()
                }
                else -> {
                    Log.d("TeamSearchFragment", "Showing search results")
                    showSearchResults()
                }
            }
        })

        viewModel.suggestions.observe(viewLifecycleOwner, Observer { suggestions ->
            Log.d("TeamSearchFragment", "Suggestions received: ${suggestions.size} suggestions")
            updateSuggestions(suggestions)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            Log.d("TeamSearchFragment", "Loading state: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                hideAllStates()
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { errorMessage ->
                Log.e("TeamSearchFragment", "Error received: $errorMessage")
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        viewModel.selectedTeamStandings.observe(viewLifecycleOwner, Observer { standings ->
            Log.d("TeamSearchFragment", "Standings received: ${standings.size} teams")
        })
    }

    private fun updateSuggestions(suggestions: List<String>) {
        Log.d("TeamSearchFragment", "updateSuggestions called with ${suggestions.size} suggestions")

        binding.suggestionsContainer.removeAllViews()

        if (suggestions.isNotEmpty()) {
            Log.d("TeamSearchFragment", "Adding ${suggestions.size} suggestion chips")
            suggestions.take(5).forEach { suggestion ->
                val chip = Chip(requireContext()).apply {
                    text = suggestion
                    isClickable = true
                    setChipBackgroundColorResource(R.color.card_background)
                    setTextColor(resources.getColor(R.color.white, null))
                    setOnClickListener {
                        Log.d("TeamSearchFragment", "Suggestion chip clicked: $suggestion")
                        binding.searchView.setQuery(suggestion, false)
                        viewModel.searchTeamByExactName(suggestion)
                        viewModel.addToSearchHistory(suggestion)
                        hideSuggestions()
                    }
                }
                binding.suggestionsContainer.addView(chip)
            }
            binding.suggestionsContainer.visibility = View.VISIBLE
        } else {
            Log.d("TeamSearchFragment", "No suggestions, hiding container")
            binding.suggestionsContainer.visibility = View.GONE
        }
    }

    private fun showSuggestions() {
        if (binding.suggestionsContainer.childCount > 0) {
            Log.d("TeamSearchFragment", "Showing suggestions")
            binding.suggestionsContainer.visibility = View.VISIBLE
        }
    }

    private fun hideSuggestions() {
        Log.d("TeamSearchFragment", "Hiding suggestions")
        binding.suggestionsContainer.visibility = View.GONE
    }

    private fun handleFavoriteClick(teamSearchResult: TeamSearchResult) {
        val team = teamSearchResult.team

        if (viewModel.isTeamFavorite(team.id)) {
            viewModel.removeFromFavorites(team.id)
            Toast.makeText(context, "${team.name} removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.addToFavorites(team.id)
            Toast.makeText(context, "${team.name} added to favorites", Toast.LENGTH_SHORT).show()
        }

        updateFavoritesCounter()
        teamSearchAdapter.notifyDataSetChanged()
    }

    private fun showStandingsBottomSheet(teamSearchResult: TeamSearchResult) {
        viewModel.loadTeamStandings(teamSearchResult)
        Toast.makeText(context, "Loading ${teamSearchResult.leagueName} standings...", Toast.LENGTH_SHORT).show()
        createStandingsBottomSheet(teamSearchResult)
    }

    private fun createStandingsBottomSheet(teamSearchResult: TeamSearchResult) {
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_standings, null)

        standingsBottomSheet = BottomSheetDialog(requireContext()).apply {
            setContentView(bottomSheetView)

            val standingsRecyclerView = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.standings_recycler_view)
            val standingsAdapter = StandingsAdapter()

            standingsRecyclerView.apply {
                adapter = standingsAdapter
                layoutManager = LinearLayoutManager(context)
            }

            val titleView = bottomSheetView.findViewById<android.widget.TextView>(R.id.standings_title)
            titleView.text = "${teamSearchResult.leagueName} Standings"

            val seasonView = bottomSheetView.findViewById<android.widget.TextView>(R.id.standings_season)
            seasonView.text = "Season ${teamSearchResult.season}"

            val closeButton = bottomSheetView.findViewById<android.widget.ImageView>(R.id.close_button)
            closeButton.setOnClickListener {
                dismiss()
            }

            show()
        }

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

                val teamPosition = viewModel.getTeamPositionInStandings(teamSearchResult.team.id)
                teamPosition?.let { position ->
                    val teamPositionView = bottomSheetView?.findViewById<android.widget.TextView>(R.id.team_position)
                    teamPositionView?.text = "${teamSearchResult.team.name} is in position ${position.rank}"
                    teamPositionView?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupClickListeners() {
        Log.d("TeamSearchFragment", "setupClickListeners called")

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.clearSearchBtn.setOnClickListener {
            clearSearch()
        }

        setupPopularTeamsChips()
        updateFavoritesCounter()

        // TEST BUTTON - Debug için
        binding.root.setOnLongClickListener {
            Log.d("TeamSearchFragment", "Long click detected, testing search")
            viewModel.testSearch()
            Toast.makeText(context, "Testing search...", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupPopularTeamsChips() {
        Log.d("TeamSearchFragment", "setupPopularTeamsChips called")

        val popularTeams = viewModel.getPopularTeams()

        popularTeams.take(6).forEach { teamName ->
            val chip = Chip(requireContext()).apply {
                text = teamName
                isClickable = true
                setChipBackgroundColorResource(R.color.accent_color)
                setTextColor(resources.getColor(R.color.white, null))
                setOnClickListener {
                    Log.d("TeamSearchFragment", "Popular team chip clicked: $teamName")
                    binding.searchView.setQuery(teamName, false)
                    viewModel.searchTeamByExactName(teamName)
                    viewModel.addToSearchHistory(teamName)
                }
            }
            binding.popularTeamsContainer.addView(chip)
        }
    }

    private fun updateFavoritesCounter() {
        val favoritesCount = viewModel.getFavoriteTeamsCount()
        binding.favoritesCountText.text = "$favoritesCount favorite teams"
        binding.favoritesCountText.visibility = if (favoritesCount > 0) View.VISIBLE else View.GONE
    }

    private fun clearSearch() {
        Log.d("TeamSearchFragment", "clearSearch called")
        binding.searchView.setQuery("", false)
        viewModel.clearSearch()
        binding.clearSearchBtn.visibility = View.GONE
        showInitialState()
        hideSuggestions()
    }

    private fun showInitialState() {
        Log.d("TeamSearchFragment", "showInitialState called")
        binding.initialStateLayout.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        updateFavoritesCounter()
    }

    private fun showEmptyState() {
        Log.d("TeamSearchFragment", "showEmptyState called")
        binding.initialStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.searchResultsRecyclerView.visibility = View.GONE
        hideSuggestions()
    }

    private fun showSearchResults() {
        Log.d("TeamSearchFragment", "showSearchResults called")
        binding.initialStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        hideSuggestions()
    }

    private fun hideInitialState() {
        binding.initialStateLayout.visibility = View.GONE
    }

    private fun hideAllStates() {
        binding.initialStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        hideSuggestions()
    }

    override fun onResume() {
        super.onResume()
        Log.d("TeamSearchFragment", "onResume called")
        teamSearchAdapter.notifyDataSetChanged()
        updateFavoritesCounter()
    }

    override fun onPause() {
        super.onPause()
        Log.d("TeamSearchFragment", "onPause called")
        standingsBottomSheet?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        Log.d("TeamSearchFragment", "onStop called")
        viewModel.clearSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("TeamSearchFragment", "onDestroyView called")
        standingsBottomSheet?.dismiss()
        standingsBottomSheet = null
        _binding = null
    }
}