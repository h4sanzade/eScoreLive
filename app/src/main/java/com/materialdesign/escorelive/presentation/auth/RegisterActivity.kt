package com.materialdesign.escorelive.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.materialdesign.escorelive.databinding.ActivityRegisterBinding
import com.materialdesign.escorelive.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.ContextCompat
import com.materialdesign.escorelive.R
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.view.View
import android.view.ViewTreeObserver

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    private var termsAccepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupUI() {
        updateRegisterButtonState(false)
        updateTermsText()
    }

    private fun updateTermsText() {
        if (termsAccepted) {
            binding.termsText.text = "✓ " + getString(R.string.read_accept_terms) + " accepted"
            binding.termsText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            binding.termsCheckbox.isChecked = true
            binding.termsCheckbox.isEnabled = false
        } else {
            binding.termsText.text = getString(R.string.read_accept_terms)
            binding.termsText.setTextColor(ContextCompat.getColor(this, R.color.accent_color))
            binding.termsCheckbox.isChecked = false
            binding.termsCheckbox.isEnabled = false
        }
    }

    private fun showTermsDialog() {
        // Find the terms overlay in the current layout
        val termsOverlay = findViewById<View>(R.id.terms_overlay)

        if (termsOverlay != null) {
            termsOverlay.visibility = View.VISIBLE

            val termsScrollView = termsOverlay.findViewById<ScrollView>(R.id.terms_scroll_view)
            val scrollInstruction = termsOverlay.findViewById<TextView>(R.id.scroll_instruction)
            val closeButton = termsOverlay.findViewById<Button>(R.id.close_button)
            val acceptButton = termsOverlay.findViewById<Button>(R.id.accept_button)

            var hasScrolledToBottom = false

            termsScrollView?.viewTreeObserver?.addOnScrollChangedListener(object : ViewTreeObserver.OnScrollChangedListener {
                override fun onScrollChanged() {
                    val view = termsScrollView.getChildAt(0)
                    if (view != null) {
                        val scrollY = termsScrollView.scrollY
                        val diff = (view.bottom - (termsScrollView.height + scrollY))

                        if (diff <= 10 && !hasScrolledToBottom) {
                            hasScrolledToBottom = true
                            enableAcceptButton(acceptButton, scrollInstruction)
                            termsScrollView.viewTreeObserver.removeOnScrollChangedListener(this)
                        }
                    }
                }
            })

            closeButton?.setOnClickListener {
                hideTermsDialog()
            }

            acceptButton?.setOnClickListener {
                if (hasScrolledToBottom) {
                    termsAccepted = true
                    updateTermsText()
                    updateRegisterButtonState(true)
                    hideTermsDialog()
                    Toast.makeText(this, "Terms accepted!", Toast.LENGTH_SHORT).show()
                }
            }

            termsOverlay.alpha = 0f
            termsOverlay.animate().alpha(1f).setDuration(300).start()
        } else {
            // Fallback: Simple dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.terms_and_conditions))
                .setMessage("By using this app, you agree to our terms and conditions. Do you accept?")
                .setPositiveButton("Accept") { _, _ ->
                    termsAccepted = true
                    updateTermsText()
                    updateRegisterButtonState(true)
                    Toast.makeText(this, "Terms accepted!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun hideTermsDialog() {
        val termsOverlay = findViewById<View>(R.id.terms_overlay)
        termsOverlay?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                termsOverlay.visibility = View.GONE
            }
            ?.start()
    }

    private fun enableAcceptButton(acceptButton: Button?, scrollInstruction: TextView?) {
        acceptButton?.isEnabled = true
        acceptButton?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_color)

        scrollInstruction?.text = "Now you can accept the terms"
        scrollInstruction?.setTextColor(ContextCompat.getColor(this, R.color.accent_color))
    }

    private fun updateRegisterButtonState(enabled: Boolean) {
        binding.registerButton.isEnabled = enabled

        if (enabled) {
            binding.registerButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_color)
            binding.registerButton.text = getString(R.string.create_account)
        } else {
            binding.registerButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            binding.registerButton.text = "Accept Terms to Continue"
        }
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(this, Observer { result ->
            when (result) {
                is RegisterResult.Success -> {
                    hideLoading()
                    showToast("Registration successful! Please login with your credentials.")
                    navigateToLogin()
                }
                is RegisterResult.Error -> {
                    hideLoading()
                    showToast("Registration failed: ${result.message}")
                }
                is RegisterResult.Loading -> {
                    showLoading()
                }
            }
        })

        viewModel.validationError.observe(this, Observer { error ->
            clearErrors()
            when (error) {
                is ValidationError.EmptyFirstName -> {
                    binding.firstNameTextInputLayout.error = getString(R.string.first_name) + " is required"
                }
                is ValidationError.EmptyLastName -> {
                    binding.lastNameTextInputLayout.error = getString(R.string.last_name) + " is required"
                }
                is ValidationError.EmptyUsername -> {
                    binding.usernameTextInputLayout.error = getString(R.string.username) + " is required"
                }
                is ValidationError.EmptyEmail -> {
                    binding.emailTextInputLayout.error = getString(R.string.email) + " is required"
                }
                is ValidationError.InvalidEmail -> {
                    binding.emailTextInputLayout.error = "Please enter a valid email"
                }
                is ValidationError.EmptyPassword -> {
                    binding.passwordTextInputLayout.error = getString(R.string.password) + " is required"
                }
                is ValidationError.WeakPassword -> {
                    binding.passwordTextInputLayout.error = "Password must be at least 6 characters"
                }
                is ValidationError.PasswordMismatch -> {
                    binding.confirmPasswordTextInputLayout.error = "Passwords do not match"
                }
                else -> {
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            if (termsAccepted) {
                val firstName = binding.firstNameEditText.text.toString().trim()
                val lastName = binding.lastNameEditText.text.toString().trim()
                val username = binding.usernameEditText.text.toString().trim()
                val email = binding.emailEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString().trim()
                val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

                clearErrors()
                viewModel.register(firstName, lastName, username, email, password, confirmPassword)
            } else {
                showTermsDialog()
            }
        }

        binding.loginTextView.setOnClickListener {
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.termsText.setOnClickListener {
            showTermsDialog()
        }

        setupErrorClearingListeners()
    }

    private fun setupErrorClearingListeners() {
        binding.firstNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.firstNameTextInputLayout.error = null
        }

        binding.lastNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.lastNameTextInputLayout.error = null
        }

        binding.usernameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.usernameTextInputLayout.error = null
        }

        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.emailTextInputLayout.error = null
        }

        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordTextInputLayout.error = null
        }

        binding.confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.confirmPasswordTextInputLayout.error = null
        }
    }

    private fun clearErrors() {
        binding.firstNameTextInputLayout.error = null
        binding.lastNameTextInputLayout.error = null
        binding.usernameTextInputLayout.error = null
        binding.emailTextInputLayout.error = null
        binding.passwordTextInputLayout.error = null
        binding.confirmPasswordTextInputLayout.error = null
    }

    private fun showLoading() {
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Creating Account..."
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.registerButton.isEnabled = termsAccepted
        updateRegisterButtonState(termsAccepted)
        binding.progressBar.visibility = View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val termsOverlay = findViewById<View>(R.id.terms_overlay)
        if (termsOverlay?.visibility == View.VISIBLE) {
            hideTermsDialog()
        } else {
            super.onBackPressed()
        }
    }
}