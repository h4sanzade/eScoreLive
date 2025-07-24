package com.materialdesign.escorelive.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.materialdesign.escorelive.databinding.ActivityLoginBinding
import com.materialdesign.escorelive.presentation.main.MainActivity
import com.materialdesign.escorelive.data.remote.AuthDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var authDataStore: AuthDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        observeViewModel()
        setupClickListeners()
        checkIfAlreadyLoggedIn()
    }

    private fun checkIfAlreadyLoggedIn() {
        lifecycleScope.launch {
            try {
                val isLoggedIn = authDataStore.isLoggedIn.first()
                if (isLoggedIn) {
                    navigateToMain()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun setupUI() {

    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this, Observer { result ->
            when (result) {
                is AuthResult.Success -> {
                    hideLoading()
                    showToast("Login successful! Welcome ${result.user.firstName}")
                    navigateToMain()
                }
                is AuthResult.Error -> {
                    hideLoading()
                    showToast("Login failed: ${result.message}")
                }
                is AuthResult.Loading -> {
                    showLoading()
                }
            }
        })

        viewModel.validationError.observe(this, Observer { error ->
            when (error) {
                is ValidationError.EmptyUsername -> {
                    binding.usernameTextInputLayout.error = "Username or email is required"
                }
                is ValidationError.EmptyPassword -> {
                    binding.passwordTextInputLayout.error = "Password is required"
                }
                else -> {
                    clearErrors()
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            clearErrors()
            viewModel.login(username, password)
        }

        binding.registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.skipLoginTextView.setOnClickListener {
            viewModel.setGuestMode(true)
            navigateToMain()
        }

        binding.root.setOnLongClickListener {
            fillDemoCredentials()
            true
        }

        binding.usernameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.usernameTextInputLayout.error = null
        }

        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordTextInputLayout.error = null
        }
    }

    private fun fillDemoCredentials() {
        binding.usernameEditText.setText("demo_user")
        binding.passwordEditText.setText("demo123")
        showToast("Demo credentials filled. Register first or use your own credentials!")
    }

    private fun clearErrors() {
        binding.usernameTextInputLayout.error = null
        binding.passwordTextInputLayout.error = null
    }

    private fun showLoading() {
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Logging in..."
        binding.progressBar.visibility = android.view.View.VISIBLE
    }

    private fun hideLoading() {
        binding.loginButton.isEnabled = true
        binding.loginButton.text = "Login"
        binding.progressBar.visibility = android.view.View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}