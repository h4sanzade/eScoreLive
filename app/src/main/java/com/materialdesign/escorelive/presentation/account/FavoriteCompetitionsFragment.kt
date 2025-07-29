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
import com.materialdesign.escorelive.databinding.FragmentFavoriteCompetitionsBinding
import com.materialdesign.escorelive.presentation.adapters.CompetitionAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoriteCompetitionsFragment : Fragment() {

    private var _binding: FragmentFavoriteCompetitionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteCompetitionsViewModel by viewModels()
    private lateinit var competitionsAdapter: CompetitionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteCompetitionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupUI()
        observeViewModel()
        setupClickListeners()

        viewModel.loadFavoriteCompetitions()
    }

    private fun setupRecyclerView() {
        competitionsAdapter = CompetitionAdapter(
            onCompetitionClick = { competition ->
                Toast.makeText(context, "Competition: ${competition.name}", Toast.LENGTH_SHORT).show()
            },
            onFavoriteClick = { competition ->
                viewModel.removeFromFavorites(competition)
            }
        )

        binding.competitionsRecyclerView.apply {
            adapter = competitionsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupUI() {
        binding.headerTitle.text = "Favorite Competitions"
    }

    private fun observeViewModel() {
        viewModel.favoriteCompetitions.observe(viewLifecycleOwner, Observer { competitions ->
            competitionsAdapter.submitList(competitions)
            updateEmptyState(competitions.isEmpty())
            binding.competitionsCount.text = "${competitions.size} competitions"
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
            binding.competitionsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.competitionsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}