package com.materialdesign.escorelive.presentation.ui.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentAccountBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.accountTitle.text = "My Account"
        binding.accountDescription.text = "Manage your profile and app preferences"
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { profile ->
        })

        viewModel.favoriteTeamsCount.observe(viewLifecycleOwner, Observer { count ->
            binding.favoriteTeamsCount.text = "$count favorite teams"
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
        binding.settingsButton.setOnClickListener {
            Toast.makeText(context, "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.favoritesButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.teamSearchFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Opening team search...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.profileButton.setOnClickListener {
            Toast.makeText(context, "Profile editing coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}