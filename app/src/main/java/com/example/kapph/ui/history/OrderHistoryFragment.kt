package com.example.kapph.ui.history // Or your chosen package

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kapph.OrderDetails
import com.example.kapph.R
import com.example.kapph.ui.history.OrderHistoryAdapter
import com.example.kapph.ui.history.OrderHistoryInteractionListener
import com.example.kapph.databinding.FragmentOrderHistoryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class OrderHistoryFragment : Fragment(), OrderHistoryInteractionListener {

    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!

    private val orderHistoryViewModel: OrderHistoryViewModel by viewModels()
    private lateinit var orderHistoryAdapter: OrderHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // Fetch order history if user is logged in
        if (Firebase.auth.currentUser != null) {
            orderHistoryViewModel.fetchOrderHistory()
        } else {
            // Handle UI for logged-out state
            binding.progressBarOrderHistory.isVisible = false
            binding.recyclerViewOrderHistory.isVisible = false
            binding.textViewNoOrders.text = "Please login to view your order history."
            binding.textViewNoOrders.isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh history if user might have logged in or placed a new order elsewhere
        // and navigated back to this screen.
        if (Firebase.auth.currentUser != null) {
            orderHistoryViewModel.fetchOrderHistory()
            binding.textViewNoOrders.text = "You have no past orders." // Reset default text
        } else {
            orderHistoryAdapter.submitList(emptyList()) // Clear adapter
            binding.progressBarOrderHistory.isVisible = false
            binding.recyclerViewOrderHistory.isVisible = false
            binding.textViewNoOrders.text = "Please login to view your order history."
            binding.textViewNoOrders.isVisible = true
        }
    }

    private fun setupRecyclerView() {
        orderHistoryAdapter = OrderHistoryAdapter(this) // Pass this fragment as the listener
        binding.recyclerViewOrderHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderHistoryAdapter
        }
    }

    private fun observeViewModel() {
        orderHistoryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarOrderHistory.isVisible = isLoading
            if (isLoading) { // Hide other views when loading
                binding.recyclerViewOrderHistory.isVisible = false
                binding.textViewNoOrders.isVisible = false
            }
        }

        orderHistoryViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                orderHistoryViewModel.clearErrorMessage() // Clear error after showing
            }
        }

        orderHistoryViewModel.orders.observe(viewLifecycleOwner) { orders ->
            Log.d("OrderHistoryFragment", "Orders updated: ${orders.size} orders received.")
            orderHistoryAdapter.submitList(orders)
            if (!orderHistoryViewModel.isLoading.value!!) { // Only update visibility if not actively loading
                binding.recyclerViewOrderHistory.isVisible = orders.isNotEmpty()
                binding.textViewNoOrders.isVisible = orders.isEmpty()
                if (orders.isEmpty() && Firebase.auth.currentUser != null) {
                    binding.textViewNoOrders.text = "You have no past orders."
                } else if (Firebase.auth.currentUser == null) {
                    binding.textViewNoOrders.text = "Please login to view your order history."
                }
            }
        }
    }

    // --- OrderHistoryInteractionListener Implementation ---
    override fun onOrderClicked(orderDetails: OrderDetails) {
        // For now, just show a Toast. Later, you could navigate to an OrderDetailScreen.
        Toast.makeText(
            requireContext(),
            "Order Clicked: ${orderDetails.orderId}\nTotal: ${orderDetails.grandTotal}",
            Toast.LENGTH_LONG
        ).show()
        Log.d("OrderHistoryFragment", "Clicked order: ${orderDetails.orderId}")
        // Example: Navigate to a (future) OrderDetailsFragment
        // val action = OrderHistoryFragmentDirections.actionOrderHistoryFragmentToOrderDetailsFragment(orderDetails.orderId)
        // findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}