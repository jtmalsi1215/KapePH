package com.example.kapph

import android.content.Intent
import android.os.Bundle
import android.util.Log // Added for logging
import android.widget.Button
import android.widget.Toast // Added for user feedback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class OnBoarding : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth // Declare Firebase Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_on_boarding)
        supportActionBar?.hide()

        // Initialize Firebase Auth
        auth = Firebase.auth

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            // Assuming your onboarding screen has a dark background if status bar icons are light
            isAppearanceLightStatusBars = false // Light icons for status bar (dark background)
            isAppearanceLightNavigationBars = true
        }

        val getStartedBtn = findViewById<Button>(R.id.primary__button)
        getStartedBtn.setOnClickListener {
            // When "Get Started" is clicked, always go to LoginActivity
            // as onStart() would have already handled auto-login if applicable.
            Log.d("OnBoarding", "Get Started clicked, navigating to LoginActivity.")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Do not finish() here if you want LoginActivity to be able to go back to OnBoarding.
            // Or finish() if OnBoarding should not be in backstack once user proceeds.
            // Let's finish for now to prevent going back to onboarding from login.
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, navigate to MainActivity
            Log.d("OnBoarding", "User already logged in: ${currentUser.email}. Navigating to MainActivity.")
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
            startActivity(intent)
            finish() // Finish OnBoardingActivity so it's not in the back stack
        } else {
            Log.d("OnBoarding", "No user logged in. OnBoarding screen will be shown.")
            // No user is signed in, OnBoarding screen remains, user will click "Get Started"
        }
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}