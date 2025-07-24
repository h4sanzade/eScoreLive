package com.materialdesign.escorelive.presentation.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.materialdesign.escorelive.databinding.ActivitySplashBinding
import com.materialdesign.escorelive.presentation.auth.LoginActivity
import com.materialdesign.escorelive.presentation.main.MainActivity
import com.materialdesign.escorelive.data.remote.AuthDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var authDataStore: AuthDataStore

    private val splashTimeOut: Long = 2500 // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        startSplashTimer()
    }

    private fun setupUI() {
        startLogoAnimation()
        startProgressAnimation()
    }

    private fun startLogoAnimation() {
        binding.appLogo.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .start()
        }

        binding.appName.apply {
            alpha = 0f
            translationY = 50f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(600)
                .start()
        }

        binding.appTagline.apply {
            alpha = 0f
            translationY = 30f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(1000)
                .start()
        }
    }

    private fun startProgressAnimation() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressIndicator.apply {
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        }, 1500)
    }

    private fun startSplashTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationAndNavigate()
        }, splashTimeOut)
    }

    private fun checkAuthenticationAndNavigate() {
        lifecycleScope.launch {
            try {
                val isLoggedIn = authDataStore.isLoggedIn.first()
                val isGuest = authDataStore.isGuestMode.first()

                when {
                    isLoggedIn -> {
                        navigateToMain()
                    }
                    isGuest -> {
                        navigateToMain()
                    }
                    else -> {
                        navigateToLogin()
                    }
                }
            } catch (e: Exception) {
                navigateToLogin()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
