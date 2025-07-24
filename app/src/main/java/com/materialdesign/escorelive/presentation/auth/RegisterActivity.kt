package com.materialdesign.escorelive.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.materialdesign.escorelive.databinding.ActivityRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets
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
        // Set hints for better UX
        binding.firstNameEditText.hint = "Enter your first name"
        binding.lastNameEditText.hint = "Enter your last name"
        binding.usernameEditText.hint = "Choose a username"
        binding.emailEditText.hint = "Enter your email"
        binding.passwordEditText.hint = "Create a password (min 6 chars)"
        binding.confirmPasswordEditText.hint = "Confirm your password"
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(this, Observer { result ->
            when (result) {
                is RegisterResult.Success -> {
                    hideLoading()
                    showToast("Registration successful! Please login with your credentials.")
                    // Navigate back to login
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
                    binding.firstNameTextInputLayout.error = "First name is required"
                }
                is ValidationError.EmptyLastName -> {
                    binding.lastNameTextInputLayout.error = "Last name is required"
                }
                is ValidationError.EmptyUsername -> {
                    binding.usernameTextInputLayout.error = "Username is required"
                }
                is ValidationError.EmptyEmail -> {
                    binding.emailTextInputLayout.error = "Email is required"
                }
                is ValidationError.InvalidEmail -> {
                    binding.emailTextInputLayout.error = "Please enter a valid email"
                }
                is ValidationError.EmptyPassword -> {
                    binding.passwordTextInputLayout.error = "Password is required"
                }
                is ValidationError.WeakPassword -> {
                    binding.passwordTextInputLayout.error = "Password must be at least 6 characters"
                }
                is ValidationError.PasswordMismatch -> {
                    binding.confirmPasswordTextInputLayout.error = "Passwords do not match"
                }
                else -> {
                    // No error, clear all
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString().trim()
            val lastName = binding.lastNameEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            clearErrors()
            viewModel.register(firstName, lastName, username, email, password, confirmPassword)
        }

        binding.loginTextView.setOnClickListener {
            finish() // Go back to login activity
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        // Clear errors when user starts typing
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
        binding.progressBar.visibility = android.view.View.VISIBLE
    }

    private fun hideLoading() {
        binding.registerButton.isEnabled = true
        binding.registerButton.text = "Create Account"
        binding.progressBar.visibility = android.view.View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToLogin() {
        // Navigate back to login activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}