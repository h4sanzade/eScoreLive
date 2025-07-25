// FilterLeaguesFragment.kt
package com.materialdesign.escorelive.presentation.filter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.materialdesign.escorelive.databinding.FragmentFilterLeaguesBinding
import com.materialdesign.escorelive.presentation.adapters.LeagueFilterAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterLeaguesFragment : Fragment() {

    private var _binding: FragmentFilterLeaguesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FilterLeaguesViewModel by viewModels()
    private lateinit var leagueAdapter: LeagueFilterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterLeaguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupUI()
        observeViewModel()
        setupClickListeners()

        // Load leagues data
        viewModel.loadLeagues()
    }

    private fun setupRecyclerView() {
        leagueAdapter = LeagueFilterAdapter(
            onLeagueClick = { league ->
                viewModel.toggleLeagueSelection(league)
            },
            onSelectionChanged = { selectedCount ->
                updateSelectedCount(selectedCount)
            }
        )

        binding.leaguesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = leagueAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupUI() {
        // Setup search
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchLeagues(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        // Observe leagues list
        viewModel.leagues.observe(viewLifecycleOwner, Observer { leagues ->
            leagueAdapter.submitList(leagues)
            updateEmptyState(leagues.isEmpty())
        })

        // Observe selected leagues
        viewModel.selectedLeagues.observe(viewLifecycleOwner, Observer { selectedLeagues ->
            leagueAdapter.updateSelectedLeagues(selectedLeagues)
            updateSelectedCount(selectedLeagues.size)
        })

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.leaguesRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        })

        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })

        // Observe save completion
        viewModel.saveCompleted.observe(viewLifecycleOwner, Observer { completed ->
            if (completed) {
                findNavController().popBackStack()
            }
        })
    }

    private fun setupClickListeners() {
        // Close button
        binding.closeButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Done button
        binding.doneButton.setOnClickListener {
            viewModel.saveSelectedLeagues()
        }

        // Select all button
        binding.selectAllButton.setOnClickListener {
            viewModel.selectAllLeagues()
        }

        // Clear all button
        binding.clearAllButton.setOnClickListener {
            viewModel.clearAllSelections()
        }
    }

    private fun updateSelectedCount(count: Int) {
        binding.selectedCountText.text = when (count) {
            0 -> "None selected"
            1 -> "1 selected"
            else -> "$count selected"
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty && !viewModel.isLoading.value!!) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.leaguesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.leaguesRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}