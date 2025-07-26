// AccountFragment.kt
package com.materialdesign.escorelive.presentation.account

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.FragmentAccountBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }

    // Permission launcher for storage access
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        // Load initial data
        viewModel.loadUserData()
    }

    private fun setupUI() {
        // Set bottom navigation selected item to account
        binding.bottomNavigation.selectedItemId = R.id.accountFragment

        // Ensure the account content is visible and others are hidden
        showAccountContent()
    }

    private fun showAccountContent() {
        // Account content is the main layout in fragment_account.xml
        // No need to toggle visibility as this is the account fragment
    }

    private fun observeViewModel() {
        // Observe user data
        viewModel.userData.observe(viewLifecycleOwner, Observer { userData ->
            userData?.let {
                binding.userFullName.text = "${it.firstName} ${it.lastName}".trim()

                // Load profile image if available
                if (it.profileImageUri.isNotEmpty()) {
                    loadProfileImage(it.profileImageUri)
                }
            }
        })

        // Observe favorites counts
        viewModel.favoriteCounts.observe(viewLifecycleOwner, Observer { counts ->
            binding.competitionsCount.text = counts.competitions.toString()
            binding.teamsCount.text = counts.teams.toString()
            binding.playersCount.text = counts.players.toString()
        })

        // Observe settings
        viewModel.appSettings.observe(viewLifecycleOwner, Observer { settings ->
            binding.notificationsSwitch.isChecked = settings.notificationsEnabled
            binding.darkThemeSwitch.isChecked = settings.darkThemeEnabled
            binding.selectedLanguageText.text = settings.selectedLanguage

            updateSelectedLeaguesText(settings.selectedLeagues)
        })

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })

        // Observe logout event
        viewModel.logoutEvent.observe(viewLifecycleOwner, Observer { shouldLogout ->
            if (shouldLogout) {
                handleLogout()
            }
        })
    }

    private fun setupClickListeners() {
        // Profile image click
        binding.profileImageCard.setOnClickListener {
            requestImagePermissionAndPick()
        }

        // Notifications switch
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNotificationsSetting(isChecked)
        }

        // Dark theme switch
        binding.darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDarkThemeSetting(isChecked)
        }

        // Filter matches row
        binding.filterMatchesRow.setOnClickListener {
            openFilterLeaguesScreen()
        }

        // Language row
        binding.languageRow.setOnClickListener {
            openLanguageSelector()
        }

        // Logout button
        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }

        // Bottom navigation - Only handle navigation between main tabs
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    try {
                        findNavController().navigate(R.id.action_account_to_home)
                    } catch (e: Exception) {
                        // Handle navigation error silently
                    }
                    true
                }
                R.id.competitionFragment -> {
                    try {
                        findNavController().navigate(R.id.action_account_to_competition)
                    } catch (e: Exception) {
                        // Handle navigation error silently
                    }
                    true
                }
                R.id.newsFragment -> {
                    try {
                        findNavController().navigate(R.id.action_account_to_news)
                    } catch (e: Exception) {
                        // Handle navigation error silently
                    }
                    true
                }
                R.id.accountFragment -> {
                    // Already here, do nothing
                    true
                }
                else -> false
            }
        }
    }

    private fun requestImagePermissionAndPick() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    context,
                    "Storage permission is needed to select a profile photo",
                    Toast.LENGTH_LONG
                ).show()
                permissionLauncher.launch(permission)
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            // Save the image URI to DataStore
            viewModel.updateProfileImage(uri.toString())

            // Load the image immediately
            loadProfileImage(uri.toString())

            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to update profile photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage(imageUri: String) {
        try {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_account)
                .error(R.drawable.ic_account)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            Glide.with(this)
                .load(imageUri)
                .apply(requestOptions)
                .into(binding.profileImage)

            // Remove tint when real image is loaded
            binding.profileImage.imageTintList = null
        } catch (e: Exception) {
            // Keep default image with tint
            binding.profileImage.setImageResource(R.drawable.ic_account)
            binding.profileImage.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_color)
        }
    }

    private fun openFilterLeaguesScreen() {
        try {
            findNavController().navigate(R.id.action_account_to_filterLeagues)
        } catch (e: Exception) {
            Toast.makeText(context, "Opening league filters...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLanguageSelector() {
        // Create a simple language selection dialog
        val languages = arrayOf("English", "Turkish", "Spanish", "French", "German")
        val currentLanguage = binding.selectedLanguageText.text.toString()
        val selectedIndex = languages.indexOf(currentLanguage)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                viewModel.updateLanguageSetting(languages[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSelectedLeaguesText(selectedLeagues: List<String>) {
        binding.selectedLeaguesText.text = when {
            selectedLeagues.isEmpty() -> "All Leagues"
            selectedLeagues.size == 1 -> selectedLeagues.first()
            selectedLeagues.size <= 3 -> selectedLeagues.joinToString(", ")
            else -> "${selectedLeagues.take(2).joinToString(", ")} +${selectedLeagues.size - 2} more"
        }
    }

    private fun handleLogout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                // Navigate to login screen or handle logout
                try {
                    findNavController().navigate(R.id.action_account_to_login)
                } catch (e: Exception) {
                    // Handle navigation error - close app or go to login activity
                    requireActivity().finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}