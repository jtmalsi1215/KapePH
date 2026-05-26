package com.example.kapph // Or your specific package

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.kapph.databinding.ActivityLoginBinding // Make sure this import is correct
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = Firebase.auth

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Crucial: Get from google-services.json or values.xml
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize the ActivityResultLauncher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            } else {
                binding.progressBarLogin.visibility = View.GONE
                binding.buttonLogin.isEnabled = true
                binding.buttonGoogleSignIn.isEnabled = true
                Log.w("LoginActivity", "Google Sign-In cancelled or failed: ${result.resultCode}")
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonLogin.setOnClickListener {
            performEmailPasswordLogin()
        }

        binding.buttonGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.textViewRegisterPrompt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            // Optional: finish()
        }

        binding.textViewForgotPassword.setOnClickListener {
            // Implement forgot password functionality if desired
            val email = binding.editTextEmailLogin.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Enter your email to reset password.", Toast.LENGTH_SHORT).show()
                binding.editTextEmailLogin.error = "Enter email here"
                binding.editTextEmailLogin.requestFocus()
            }
        }
    }

    private fun performEmailPasswordLogin() {
        val email = binding.editTextEmailLogin.text.toString().trim()
        val password = binding.editTextPasswordLogin.text.toString().trim()

        if (email.isEmpty()) {
            binding.editTextEmailLogin.error = "Email is required"
            binding.editTextEmailLogin.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmailLogin.error = "Enter a valid email"
            binding.editTextEmailLogin.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.editTextPasswordLogin.error = "Password is required"
            binding.editTextPasswordLogin.requestFocus()
            return
        }

        binding.progressBarLogin.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        binding.buttonGoogleSignIn.isEnabled = false


        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarLogin.visibility = View.GONE
                binding.buttonLogin.isEnabled = true
                binding.buttonGoogleSignIn.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithEmail:success")
                    navigateToMainApp()
                } else {
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        binding.progressBarLogin.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        binding.buttonGoogleSignIn.isEnabled = false
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d("LoginActivity", "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign in failed", e)
            binding.progressBarLogin.visibility = View.GONE
            binding.buttonLogin.isEnabled = true
            binding.buttonGoogleSignIn.isEnabled = true
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBarLogin.visibility = View.GONE
                binding.buttonLogin.isEnabled = true
                binding.buttonGoogleSignIn.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithCredential_Google:success")
                    navigateToMainApp()
                } else {
                    Log.w("LoginActivity", "signInWithCredential_Google:failure", task.exception)
                    Toast.makeText(baseContext, "Google Authentication Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.progressBarLogin.visibility = View.VISIBLE
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBarLogin.visibility = View.GONE
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Password reset email sent.")
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("LoginActivity", "Error sending password reset email", task.exception)
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun navigateToMainApp() {
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        // If you want to auto-login if already signed in, uncomment this,
        // but typically LoginActivity is shown when user is explicitly logged out or not logged in.
        // val currentUser = auth.currentUser
        // if (currentUser != null) {
        //    navigateToMainApp()
        // }
    }
}