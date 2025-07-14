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

@AndroidEntryPoint
class MatchDetailFragment : Fragment() {

    private var _binding: FragmentMatchDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MatchDetailViewModel by viewModels()
    private lateinit var eventsAdapter: MatchEventsAdapter
    private lateinit var lineupAdapter: LineupAdapter

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

        binding.eventsRecyclerView.apply {
            adapter = eventsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.lineupRecyclerView.apply {
            adapter = lineupAdapter
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
    }

    private fun showEventsSection() {
        binding.eventsSection.visibility = View.VISIBLE
        binding.lineupSection.visibility = View.GONE
        binding.statisticsSection.visibility = View.GONE

        binding.eventsTab.setBackgroundResource(R.drawable.selected_day)
        binding.lineupTab.background = null
        binding.statisticsTab.background = null
    }

    private fun showLineupSection() {
        binding.eventsSection.visibility = View.GONE
        binding.lineupSection.visibility = View.VISIBLE
        binding.statisticsSection.visibility = View.GONE

        binding.eventsTab.background = null
        binding.lineupTab.setBackgroundResource(R.drawable.selected_day)
        binding.statisticsTab.background = null
    }

    private fun showStatisticsSection() {
        binding.eventsSection.visibility = View.GONE
        binding.lineupSection.visibility = View.GONE
        binding.statisticsSection.visibility = View.VISIBLE

        binding.eventsTab.background = null
        binding.lineupTab.background = null
        binding.statisticsTab.setBackgroundResource(R.drawable.selected_day)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}