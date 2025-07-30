// AccountFragment.kt - Fixed with correct binding references
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
import com.materialdesign.escorelive.utils.LocaleManager
import com.materialdesign.escorelive.utils.LanguageSelectorDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    @Inject
    lateinit var localeManager: LocaleManager

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
            Toast.makeText(context, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
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
        updateLanguageDisplay()
    }

    private fun showAccountContent() {
        // Fix: The layout uses different id names. Looking at fragment_account.xml,
        // the text views are embedded in the cards and have specific layouts.
        // We should only set text for views that actually exist in the layout.

        // These text views don't exist in the layout, so we remove them:
        // - myAccountText, myFavoritesText, generalSettingsText, otherText
        // - competitionsLabel, teamsLabel, playersLabel
        // - notificationsLabel, darkThemeLabel, filterMatchesLabel, languageLabel

        // The actual text content is already set in the layout XML files
        // Only dynamic content needs to be set programmatically

        // Set logout button text (this exists in the layout)
        binding.logoutButton.text = getString(R.string.log_out)
    }

    private fun updateLanguageDisplay() {
        try {
            val currentLanguage = localeManager.getLanguage(requireContext())
            val languageDisplayName = localeManager.getLanguageDisplayName(requireContext(), currentLanguage)
            binding.selectedLanguageText.text = languageDisplayName
            Log.d("AccountFragment", "Current language: $currentLanguage, Display: $languageDisplayName")
        } catch (e: Exception) {
            Log.e("AccountFragment", "Error updating language display", e)
            binding.selectedLanguageText.text = getString(R.string.english)
        }
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

        viewModel.languageChanged.observe(viewLifecycleOwner, Observer { languageChanged ->
            if (languageChanged) {
                Log.d("AccountFragment", "Language changed detected, restarting activity")
                requireActivity().recreate()
                viewModel.clearLanguageChanged()
            }
        })
    }

    private fun setupClickListeners() {
        Log.d("AccountFragment", "Setting up click listeners")

        binding.profileImageCard.setOnClickListener {
            requestImagePermissionAndPick()
        }

        // Make the entire competition section clickable
        binding.competitionsCount.setOnClickListener {
            Log.d("AccountFragment", "Competitions count clicked!")
            navigateToFavoriteCompetitions()
        }

        // Also make the parent container clickable for competitions
        binding.competitionsCount.parent?.let { parent ->
            (parent as? View)?.setOnClickListener {
                Log.d("AccountFragment", "Competitions section clicked!")
                navigateToFavoriteCompetitions()
            }
        }

        // Make the entire teams section clickable
        binding.teamsCount.setOnClickListener {
            Log.d("AccountFragment", "Teams count clicked!")
            navigateToFavoriteTeams()
        }

        // Also make the parent container clickable for teams
        binding.teamsCount.parent?.let { parent ->
            (parent as? View)?.setOnClickListener {
                Log.d("AccountFragment", "Teams section clicked!")
                navigateToFavoriteTeams()
            }
        }

        // Make the entire players section clickable
        binding.playersCount.setOnClickListener {
            Log.d("AccountFragment", "Players count clicked!")
            Toast.makeText(context, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Also make the parent container clickable for players
        binding.playersCount.parent?.let { parent ->
            (parent as? View)?.setOnClickListener {
                Log.d("AccountFragment", "Players section clicked!")
                Toast.makeText(context, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
            }
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
            Log.d("AccountFragment", "Language row clicked!")
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

            // Show immediate feedback
            Toast.makeText(context, getString(R.string.competitions) + " - " + getString(R.string.loading), Toast.LENGTH_SHORT).show()

            // Try navigation with proper action
            findNavController().navigate(R.id.action_account_to_favoriteCompetitions)
            Log.d("AccountFragment", "Navigation to favorite competitions successful")

        } catch (e: Exception) {
            Log.e("AccountFragment", "Navigation error to favorite competitions", e)

            // Show error and fallback dialog
            Toast.makeText(context, getString(R.string.error_occurred) + ": Navigation failed", Toast.LENGTH_LONG).show()

            // Show a fallback dialog with competitions info
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.favorite_competitions))
                .setMessage("Favorite competitions feature is available. Navigation issue detected.")
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    // Try alternative navigation or open a simple screen
                    showComingSoonMessage("Favorite Competitions")
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun navigateToFavoriteTeams() {
        try {
            Log.d("AccountFragment", "Attempting to navigate to favorite teams")

            // Show immediate feedback
            Toast.makeText(context, getString(R.string.teams) + " - " + getString(R.string.loading), Toast.LENGTH_SHORT).show()

            // Try navigation with proper action
            findNavController().navigate(R.id.action_account_to_favoriteTeams)
            Log.d("AccountFragment", "Navigation to favorite teams successful")

        } catch (e: Exception) {
            Log.e("AccountFragment", "Navigation error to favorite teams", e)

            // Show error and fallback dialog
            Toast.makeText(context, getString(R.string.error_occurred) + ": Navigation failed", Toast.LENGTH_LONG).show()

            // Show a fallback dialog with teams info
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.favorite_teams))
                .setMessage("Favorite teams feature is available. Navigation issue detected.")
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    // Try alternative navigation or open a simple screen
                    showComingSoonMessage("Favorite Teams")
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun showComingSoonMessage(feature: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(feature)
            .setMessage("$feature screen will open here. Currently showing placeholder.")
            .setPositiveButton(getString(R.string.ok), null)
            .show()
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
                    getString(R.string.storage_permission_needed),
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
            Toast.makeText(context, getString(R.string.profile_photo_updated), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, getString(R.string.loading) + "...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLanguageSelector() {
        Log.d("AccountFragment", "Opening language selector...")

        try {
            val languages = arrayOf(getString(R.string.english), "Azərbaycan")
            val languageCodes = arrayOf("en", "az")
            val currentLanguageCode = localeManager.getLanguage(requireContext())
            val selectedIndex = languageCodes.indexOf(currentLanguageCode)

            Log.d("AccountFragment", "Current language code: $currentLanguageCode, Selected index: $selectedIndex")

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                    val selectedLanguageCode = languageCodes[which]
                    val selectedLanguageName = languages[which]

                    Log.d("AccountFragment", "Selected language: $selectedLanguageName ($selectedLanguageCode)")

                    try {
                        localeManager.setLocale(requireContext(), selectedLanguageCode)
                        viewModel.updateLanguageSettings(selectedLanguageCode, selectedLanguageName)
                        Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        requireActivity().recreate()

                    } catch (e: Exception) {
                        Log.e("AccountFragment", "Error changing language", e)
                        Toast.makeText(requireContext(), getString(R.string.error_occurred) + ": ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

        } catch (e: Exception) {
            Log.e("AccountFragment", "Error opening language selector", e)
            Toast.makeText(requireContext(), getString(R.string.error_occurred) + ": ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateSelectedLeaguesText(selectedLeagues: List<String>) {
        binding.selectedLeaguesText.text = when {
            selectedLeagues.isEmpty() -> getString(R.string.all_leagues)
            selectedLeagues.size == 1 -> selectedLeagues.first()
            selectedLeagues.size <= 3 -> selectedLeagues.joinToString(", ")
            else -> "${selectedLeagues.take(2).joinToString(", ")} +${selectedLeagues.size - 2} " + getString(R.string.more)
        }
    }

    private fun handleLogout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.log_out))
            .setMessage(getString(R.string.logout_confirmation))
            .setPositiveButton(getString(R.string.log_out)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performLogout() {
        try {
            findNavController().navigate(
                R.id.action_account_to_login,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        } catch (e: Exception) {
            Log.e("AccountFragment", "Navigation error during logout", e)

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