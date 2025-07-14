package com.materialdesign.escorelive.ui.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.materialdesign.escorelive.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: TeamSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        searchAdapter = TeamSearchAdapter { team ->
            viewModel.toggleFavorite(team)
        }

        binding.searchResultsRecycler.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { text ->
            val query = text.toString().trim()

            if (query.isEmpty()) {
                binding.clearSearch.visibility = View.GONE
                viewModel.clearSearch()
            } else {
                binding.clearSearch.visibility = View.VISIBLE
                viewModel.searchTeams(query)
            }
        }

        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchTeams(query)
            }
            true
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.clearSearch.setOnClickListener {
            binding.searchEditText.text?.clear()
            binding.clearSearch.visibility = View.GONE
            viewModel.clearSearch()
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner, Observer { results ->
            searchAdapter.submitList(results)
            updateEmptyState(results.isEmpty() && binding.searchEditText.text.toString().isNotEmpty())
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.searchResultsRecycler.visibility = View.GONE
            binding.emptyMessage.text = "No teams found"
        } else if (binding.searchEditText.text.toString().isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.searchResultsRecycler.visibility = View.GONE
            binding.emptyMessage.text = "Search for teams to add to favorites"
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.searchResultsRecycler.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}