package com.example.kapph.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kapph.LoginActivity // Import LoginActivity
import com.example.kapph.R
import com.example.kapph.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn // For Google Sign-Out
import com.google.android.gms.auth.api.signin.GoogleSignInClient // For Google Sign-Out
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // For Google Sign-Out
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign-In options (needed for sign-out)
        // Use the same R.string.default_web_client_id you used in LoginActivity
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.example.kapph.R.string.default_web_client_id)) // Make sure R is imported correctly
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Display user info
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.textViewUserEmail.text = currentUser.email ?: "No email available"
            // You could also display currentUser.displayName if available
            // and load currentUser.photoUrl into binding.imageViewProfileIcon using Glide/Picasso
        } else {
            // Should not happen if ProfileFragment is only accessible when logged in,
            // but good to handle.
            binding.textViewUserEmail.text = "Not logged in"
            binding.buttonLogout.isEnabled = false // Disable logout if not logged in
        }

        // Set up logout button
        binding.buttonLogout.setOnClickListener {
            performLogout()
        }

        binding.buttonOrderHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_order_history)
        }
    }

    private fun performLogout() {
        // Firebase sign out
        auth.signOut()

        // Google sign out (important if user signed in with Google)
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            // Optional: Handle completion of Google sign out
            Log.d("ProfileFragment", "Google Sign-Out complete.")
        }

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate to LoginActivity and clear back stack
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finishAffinity() // Finish MainActivity and any other activities in the task
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}