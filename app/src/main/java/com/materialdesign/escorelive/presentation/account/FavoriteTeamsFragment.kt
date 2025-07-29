package com.materialdesign.escorelive.presentation.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentFavoriteTeamsBinding
import com.materialdesign.escorelive.presentation.adapters.TeamSearchAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoriteTeamsFragment : Fragment() {

    private var _binding: FragmentFavoriteTeamsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteTeamsViewModel by viewModels()
    private lateinit var teamsAdapter: TeamSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupUI()
        observeViewModel()
        setupClickListeners()

        viewModel.loadFavoriteTeams()
    }

    private fun setupRecyclerView() {
        teamsAdapter = TeamSearchAdapter(
            onTeamClick = { teamSearchResult ->
                Toast.makeText(context, "Team: ${teamSearchResult.team.name}", Toast.LENGTH_SHORT).show()
            },
            onFavoriteClick = { teamSearchResult ->
                viewModel.removeFromFavorites(teamSearchResult.team.id)
            },
            onStandingsClick = { teamSearchResult ->
                Toast.makeText(context, "Standings for ${teamSearchResult.team.name}", Toast.LENGTH_SHORT).show()
            },
            isTeamFavorite = { teamId ->
                viewModel.isTeamFavorite(teamId)
            }
        )

        binding.teamsRecyclerView.apply {
            adapter = teamsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupUI() {
        binding.headerTitle.text = "Favorite Teams"
    }

    private fun observeViewModel() {
        viewModel.favoriteTeams.observe(viewLifecycleOwner, Observer { teams ->
            teamsAdapter.submitList(teams)
            updateEmptyState(teams.isEmpty())
            binding.teamsCount.text = "${teams.size} teams"
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

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.teamsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.teamsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}