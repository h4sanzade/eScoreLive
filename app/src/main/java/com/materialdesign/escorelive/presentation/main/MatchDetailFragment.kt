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
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentMatchDetailBinding
import com.materialdesign.escorelive.domain.model.Match
import dagger.hilt.android.AndroidEntryPoint
import com.materialdesign.escorelive.presentation.adapters.LineupAdapter
import com.materialdesign.escorelive.presentation.adapters.MatchEventsAdapter
import com.materialdesign.escorelive.presentation.adapters.H2HAdapter
import com.materialdesign.escorelive.presentation.adapters.StandingsAdapter

@AndroidEntryPoint
class MatchDetailFragment : Fragment() {

    private var _binding: FragmentMatchDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MatchDetailViewModel by viewModels()
    private lateinit var eventsAdapter: MatchEventsAdapter
    private lateinit var lineupAdapter: LineupAdapter
    private lateinit var h2hAdapter: H2HAdapter
    private lateinit var standingsAdapter: StandingsAdapter

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

        viewModel.loadMatchDetails(args.matchId)
    }

    private fun setupRecyclerViews() {
        eventsAdapter = MatchEventsAdapter()
        lineupAdapter = LineupAdapter()
        h2hAdapter = H2HAdapter()
        standingsAdapter = StandingsAdapter()

        binding.eventsRecyclerView.apply {
            adapter = eventsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.lineupRecyclerView.apply {
            adapter = lineupAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.h2hRecyclerView.apply {
            adapter = h2hAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.standingsRecyclerView.apply {
            adapter = standingsAdapter
            layoutManager = LinearLayoutManager(context)
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

        // H2H Observer
        viewModel.h2hMatches.observe(viewLifecycleOwner, Observer { h2hMatches ->
            if (h2hMatches.isNotEmpty()) {
                h2hAdapter.submitList(h2hMatches)
                binding.h2hSection.visibility = View.VISIBLE
                binding.h2hEmptyState.visibility = View.GONE
            } else {
                binding.h2hSection.visibility = View.GONE
                binding.h2hEmptyState.visibility = View.VISIBLE
            }
        })

        // Standings Observer
        viewModel.standings.observe(viewLifecycleOwner, Observer { standings ->
            if (standings.isNotEmpty()) {
                standingsAdapter.submitList(standings)
                binding.standingsSection.visibility = View.VISIBLE
                binding.standingsEmptyState.visibility = View.GONE
            } else {
                binding.standingsSection.visibility = View.GONE
                binding.standingsEmptyState.visibility = View.VISIBLE
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

    private fun updateMatchInfo(match: Match) {
        with(binding) {
            // Teams
            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name

            Glide.with(this@MatchDetailFragment)
                .load(match.homeTeam.logo)
                .placeholder(R.drawable.ic_placeholder)
                .into(homeTeamLogo)

            Glide.with(this@MatchDetailFragment)
                .load(match.awayTeam.logo)
                .placeholder(R.drawable.ic_placeholder)
                .into(awayTeamLogo)

            homeScore.text = match.homeScore.toString()
            awayScore.text = match.awayScore.toString()

            matchStatus.text = match.matchStatus
            matchMinute.text = match.matchMinute

            leagueName.text = match.league.name
            Glide.with(this@MatchDetailFragment)
                .load(match.league.logo)
                .placeholder(R.drawable.ic_placeholder)
                .into(leagueLogo)

            kickoffTime.text = match.kickoffTimeFormatted ?: "TBD"
        }
    }

    private fun updateStatistics(stats: MatchStatistics) {
        with(binding) {
            homePossession.text = "${stats.homePossession}%"
            awayPossession.text = "${stats.awayPossession}%"
            possessionProgressBar.progress = stats.homePossession

            homeShots.text = stats.homeShots.toString()
            awayShots.text = stats.awayShots.toString()

            homeShotsOnTarget.text = stats.homeShotsOnTarget.toString()
            awayShotsOnTarget.text = stats.awayShotsOnTarget.toString()

            homeCorners.text = stats.homeCorners.toString()
            awayCorners.text = stats.awayCorners.toString()

            homeYellowCards.text = stats.homeYellowCards.toString()
            awayYellowCards.text = stats.awayYellowCards.toString()

            homeRedCards.text = stats.homeRedCards.toString()
            awayRedCards.text = stats.awayRedCards.toString()

            statisticsSection.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

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
        hideAllSections()
        binding.eventsSection.visibility = View.VISIBLE
        updateTabSelection(binding.eventsTab)
    }

    private fun showLineupSection() {
        hideAllSections()
        binding.lineupSection.visibility = View.VISIBLE
        updateTabSelection(binding.lineupTab)
    }

    private fun showStatisticsSection() {
        hideAllSections()
        binding.statisticsSection.visibility = View.VISIBLE
        updateTabSelection(binding.statisticsTab)
    }

    private fun showH2HSection() {
        hideAllSections()
        binding.h2hSection.visibility = View.VISIBLE
        binding.h2hEmptyState.visibility = View.GONE
        updateTabSelection(binding.h2hTab)
    }

    private fun showStandingsSection() {
        hideAllSections()
        binding.standingsSection.visibility = View.VISIBLE
        binding.standingsEmptyState.visibility = View.GONE
        updateTabSelection(binding.standingsTab)
    }

    private fun hideAllSections() {
        binding.eventsSection.visibility = View.GONE
        binding.lineupSection.visibility = View.GONE
        binding.statisticsSection.visibility = View.GONE
        binding.h2hSection.visibility = View.GONE
        binding.h2hEmptyState.visibility = View.GONE
        binding.standingsSection.visibility = View.GONE
        binding.standingsEmptyState.visibility = View.GONE
    }

    private fun updateTabSelection(selectedTab: View) {
        binding.eventsTab.background = null
        binding.lineupTab.background = null
        binding.statisticsTab.background = null
        binding.h2hTab.background = null
        binding.standingsTab.background = null

        selectedTab.setBackgroundResource(R.drawable.selected_day)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}