package com.materialdesign.escorelive.ui.matchdetail

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
import com.bumptech.glide.Glide
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentMatchDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MatchDetailFragment : Fragment() {

    private var _binding: FragmentMatchDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MatchDetailViewModel by viewModels()
    private lateinit var eventsAdapter: MatchEventsAdapter
    private lateinit var lineupAdapter: LineupAdapter
    private lateinit var h2hAdapter: H2HAdapter
    private lateinit var standingsAdapter: StandingsAdapter

    // Navigation Component safe args
    private val args: MatchDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
        setupClickListeners()

        // Load match details using safe args
        viewModel.loadMatchDetails(args.matchId)
    }

    private fun setupRecyclerViews() {
        // Events/Summary RecyclerView
        eventsAdapter = MatchEventsAdapter()
        binding.eventsRecyclerView.apply {
            adapter = eventsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        // Lineup RecyclerView
        lineupAdapter = LineupAdapter()
        binding.lineupRecyclerView.apply {
            adapter = lineupAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        // H2H RecyclerView
        h2hAdapter = H2HAdapter()
        binding.h2hRecyclerView.apply {
            adapter = h2hAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        // Standings RecyclerView
        standingsAdapter = StandingsAdapter()
        binding.standingsRecyclerView.apply {
            adapter = standingsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.matchDetails.observe(viewLifecycleOwner, Observer { match ->
            match?.let { updateMatchInfo(it) }
        })

        viewModel.matchEvents.observe(viewLifecycleOwner, Observer { events ->
            eventsAdapter.submitList(events)
            binding.eventsSection.visibility = if (events.isNotEmpty()) View.VISIBLE else View.GONE
        })

        viewModel.matchLineup.observe(viewLifecycleOwner, Observer { lineup ->
            lineupAdapter.submitList(lineup)
            binding.lineupSection.visibility = if (lineup.isNotEmpty()) View.VISIBLE else View.GONE
        })

        viewModel.matchStatistics.observe(viewLifecycleOwner, Observer { stats ->
            stats?.let { updateStatistics(it) }
        })

        viewModel.h2hMatches.observe(viewLifecycleOwner, Observer { matches ->
            if (matches.isNotEmpty()) {
                h2hAdapter.submitList(matches)
                binding.h2hSection.visibility = View.VISIBLE
            } else {
                binding.h2hSection.visibility = View.GONE
            }
        })

        viewModel.standings.observe(viewLifecycleOwner, Observer { standings ->
            if (standings.isNotEmpty()) {
                standingsAdapter.submitList(standings)
                binding.standingsSection.visibility = View.VISIBLE
            } else {
                binding.standingsSection.visibility = View.GONE
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })
    }

    private fun updateMatchInfo(match: LiveMatch) {
        with(binding) {
            // Teams
            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name

            Glide.with(this@MatchDetailFragment)
                .load(match.homeTeam.logo)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(homeTeamLogo)

            Glide.with(this@MatchDetailFragment)
                .load(match.awayTeam.logo)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(awayTeamLogo)

            // Score
            homeScore.text = match.homeScore.toString()
            awayScore.text = match.awayScore.toString()

            // Match status
            matchStatus.text = match.matchStatus
            matchMinute.text = match.matchMinute

            // League
            leagueName.text = match.league.name
            Glide.with(this@MatchDetailFragment)
                .load(match.league.logo)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(leagueLogo)

            // Date and time
            kickoffTime.text = match.kickoffTimeFormatted ?: "TBD"
        }
    }

    private fun updateStatistics(stats: MatchStatistics) {
        with(binding) {
            // Ball possession
            homePossession.text = "${stats.homePossession}%"
            awayPossession.text = "${stats.awayPossession}%"
            possessionProgressBar.progress = stats.homePossession

            // Shots
            homeShots.text = stats.homeShots.toString()
            awayShots.text = stats.awayShots.toString()

            // Shots on target
            homeShotsOnTarget.text = stats.homeShotsOnTarget.toString()
            awayShotsOnTarget.text = stats.awayShotsOnTarget.toString()

            // Corners
            homeCorners.text = stats.homeCorners.toString()
            awayCorners.text = stats.awayCorners.toString()

            // Yellow cards
            homeYellowCards.text = stats.homeYellowCards.toString()
            awayYellowCards.text = stats.awayYellowCards.toString()

            // Red cards
            homeRedCards.text = stats.homeRedCards.toString()
            awayRedCards.text = stats.awayRedCards.toString()

            statisticsSection.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Tab click listeners
        binding.eventsTab.setOnClickListener {
            showEventsSection()
        }

        binding.lineupTab.setOnClickListener {
            showLineupSection()
        }

        binding.statisticsTab.setOnClickListener {
            showStatisticsSection()
        }

        binding.h2hTab.setOnClickListener {
            showH2HSection()
        }

        binding.standingsTab.setOnClickListener {
            showStandingsSection()
        }
    }

    private fun showEventsSection() {
        // Hide all sections
        hideAllSections()

        // Show events section
        binding.eventsSection.visibility = View.VISIBLE

        // Update tab appearance
        resetAllTabs()
        binding.eventsTab.setBackgroundResource(R.drawable.selected_day)
    }

    private fun showLineupSection() {
        // Hide all sections
        hideAllSections()

        // Show lineup section
        binding.lineupSection.visibility = View.VISIBLE

        // Update tab appearance
        resetAllTabs()
        binding.lineupTab.setBackgroundResource(R.drawable.selected_day)
    }

    private fun showStatisticsSection() {
        // Hide all sections
        hideAllSections()

        // Show statistics section
        binding.statisticsSection.visibility = View.VISIBLE

        // Update tab appearance
        resetAllTabs()
        binding.statisticsTab.setBackgroundResource(R.drawable.selected_day)
    }

    private fun showH2HSection() {
        // Hide all sections
        hideAllSections()

        // Show H2H section if data is available
        if (viewModel.h2hMatches.value?.isNotEmpty() == true) {
            binding.h2hSection.visibility = View.VISIBLE
        } else {
            binding.h2hEmptyState.visibility = View.VISIBLE
        }

        // Update tab appearance
        resetAllTabs()
        binding.h2hTab.setBackgroundResource(R.drawable.selected_day)
    }

    private fun showStandingsSection() {
        // Hide all sections
        hideAllSections()

        // Show standings section if data is available
        if (viewModel.standings.value?.isNotEmpty() == true) {
            binding.standingsSection.visibility = View.VISIBLE
        } else {
            binding.standingsEmptyState.visibility = View.VISIBLE
        }

        // Update tab appearance
        resetAllTabs()
        binding.standingsTab.setBackgroundResource(R.drawable.selected_day)
    }

    private fun hideAllSections() {
        binding.eventsSection.visibility = View.GONE
        binding.lineupSection.visibility = View.GONE
        binding.statisticsSection.visibility = View.GONE
        binding.h2hSection.visibility = View.GONE
        binding.standingsSection.visibility = View.GONE
        binding.h2hEmptyState.visibility = View.GONE
        binding.standingsEmptyState.visibility = View.GONE
    }

    private fun resetAllTabs() {
        binding.eventsTab.background = null
        binding.lineupTab.background = null
        binding.statisticsTab.background = null
        binding.h2hTab.background = null
        binding.standingsTab.background = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}