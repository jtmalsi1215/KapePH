package com.example.kapph // Or your specific package for activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kapph.databinding.ActivityRegisterBinding // Make sure this import is correct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide() // If you want to hide the action bar for this activity

        // Initialize Firebase Auth
        auth = Firebase.auth

        binding.buttonRegister.setOnClickListener {
            performRegistration()
        }

        binding.textViewLoginPrompt.setOnClickListener {
            // Navigate to LoginActivity (we'll create this next)
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Optional: finish RegisterActivity so user can't go back to it from Login
        }
    }

    private fun performRegistration() {
        val email = binding.editTextEmailRegister.text.toString().trim()
        val password = binding.editTextPasswordRegister.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPasswordRegister.text.toString().trim()

        if (email.isEmpty()) {
            binding.editTextEmailRegister.error = "Email is required"
            binding.editTextEmailRegister.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmailRegister.error = "Enter a valid email"
            binding.editTextEmailRegister.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.editTextPasswordRegister.error = "Password is required"
            binding.editTextPasswordRegister.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.editTextPasswordRegister.error = "Password must be at least 6 characters"
            binding.editTextPasswordRegister.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.editTextConfirmPasswordRegister.error = "Confirm password is required"
            binding.editTextConfirmPasswordRegister.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.editTextConfirmPasswordRegister.error = "Passwords do not match"
            binding.editTextConfirmPasswordRegister.requestFocus()
            return
        }

        // All validations passed, proceed with Firebase registration
        binding.progressBarRegister.visibility = View.VISIBLE
        binding.buttonRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarRegister.visibility = View.GONE
                binding.buttonRegister.isEnabled = true
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("RegisterActivity", "createUserWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Registration successful. Please login.", Toast.LENGTH_LONG).show()

                    // Optionally send email verification
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d("RegisterActivity", "Verification email sent.")
                        } else {
                            Log.e("RegisterActivity", "sendEmailVerification", verificationTask.exception)
                        }
                    }

                    // Navigate to LoginActivity or directly to MainActivity if auto-login after register is desired
                    // For now, let's go to LoginActivity
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity() // Finish this and all parent activities up to Login
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }
}