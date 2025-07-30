
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
import android.util.Log
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

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }

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

        viewModel.loadUserData()
    }

    private fun setupUI() {
        binding.bottomNavigation.selectedItemId = R.id.accountFragment
        showAccountContent()
    }

    private fun showAccountContent() {
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner, Observer { userData ->
            userData?.let {
                binding.userFullName.text = "${it.firstName} ${it.lastName}".trim()
                binding.usernameText.text = "@${it.username}"

                if (it.profileImageUri.isNotEmpty()) {
                    loadProfileImage(it.profileImageUri)
                }
            }
        })

        viewModel.favoriteCounts.observe(viewLifecycleOwner, Observer { counts ->
            binding.competitionsCount.text = counts.competitions.toString()
            binding.teamsCount.text = counts.teams.toString()
            binding.playersCount.text = counts.players.toString()
        })

        viewModel.appSettings.observe(viewLifecycleOwner, Observer { settings ->
            binding.notificationsSwitch.isChecked = settings.notificationsEnabled
            binding.darkThemeSwitch.isChecked = settings.darkThemeEnabled
            binding.selectedLanguageText.text = settings.selectedLanguage

            updateSelectedLeaguesText(settings.selectedLeagues)
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

        viewModel.logoutEvent.observe(viewLifecycleOwner, Observer { shouldLogout ->
            if (shouldLogout) {
                handleLogout()
            }
        })
    }

    private fun setupClickListeners() {
        Log.d("AccountFragment", "Setting up click listeners")

        binding.profileImageCard.setOnClickListener {
            requestImagePermissionAndPick()
        }

        binding.competitionsCount.setOnClickListener {
            Log.d("AccountFragment", "Competitions count clicked!")
            Toast.makeText(context, "Competitions clicked - navigating...", Toast.LENGTH_SHORT).show()
            navigateToFavoriteCompetitions()
        }

        binding.teamsCount.setOnClickListener {
            Log.d("AccountFragment", "Teams count clicked!")
            Toast.makeText(context, "Teams clicked - navigating...", Toast.LENGTH_SHORT).show()
            navigateToFavoriteTeams()
        }

        binding.playersCount.setOnClickListener {
            Log.d("AccountFragment", "Players count clicked!")
            Toast.makeText(context, "Players feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNotificationsSetting(isChecked)
        }

        binding.darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDarkThemeSetting(isChecked)
        }

        binding.filterMatchesRow.setOnClickListener {
            openFilterLeaguesScreen()
        }

        binding.languageRow.setOnClickListener {
            openLanguageSelector()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    try {
                        findNavController().navigate(R.id.action_account_to_home)
                    } catch (e: Exception) {
                        Log.e("AccountFragment", "Navigation error to home", e)
                    }
                    true
                }
                R.id.competitionFragment -> {
                    try {
                        findNavController().navigate(R.id.action_account_to_competition)
                    } catch (e: Exception) {
                        Log.e("AccountFragment", "Navigation error to competition", e)
                    }
                    true
                }
                R.id.newsFragment -> {
                    try {
                        findNavController().navigate(R.id.action_account_to_news)
                    } catch (e: Exception) {
                        Log.e("AccountFragment", "Navigation error to news", e)
                    }
                    true
                }
                R.id.accountFragment -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToFavoriteCompetitions() {
        try {
            Log.d("AccountFragment", "Attempting to navigate to favorite competitions")
            findNavController().navigate(R.id.action_account_to_favoriteCompetitions)
            Log.d("AccountFragment", "Navigation to favorite competitions successful")
        } catch (e: Exception) {
            Log.e("AccountFragment", "Navigation error to favorite competitions", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Favorite Competitions")
                .setMessage("Feature under development. Coming soon!")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun navigateToFavoriteTeams() {
        try {
            Log.d("AccountFragment", "Attempting to navigate to favorite teams")
            findNavController().navigate(R.id.action_account_to_favoriteTeams)
            Log.d("AccountFragment", "Navigation to favorite teams successful")
        } catch (e: Exception) {
            Log.e("AccountFragment", "Navigation error to favorite teams", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Favorite Teams")
                .setMessage("Feature under development. Coming soon!")
                .setPositiveButton("OK", null)
                .show()
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
            viewModel.updateProfileImage(uri.toString())
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

            binding.profileImage.imageTintList = null
        } catch (e: Exception) {
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
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            // Navigate to login and clear the back stack
            findNavController().navigate(
                R.id.action_account_to_login,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        } catch (e: Exception) {
            Log.e("AccountFragment", "Navigation error during logout", e)

            // Fallback: Start login activity and finish current activity
            try {
                val intent = Intent(requireContext(),
                    Class.forName("com.materialdesign.escorelive.presentation.auth.LoginActivity"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } catch (ex: Exception) {
                Log.e("AccountFragment", "Failed to start login activity", ex)
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}