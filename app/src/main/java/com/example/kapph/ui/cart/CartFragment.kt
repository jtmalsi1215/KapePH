package com.example.kapph.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Use activityViewModels for shared ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kapph.CartItem
import com.example.kapph.R // Main R import
import com.example.kapph.databinding.FragmentCartBinding
import com.example.kapph.OrderActivity // Assuming 'order' is your OrderActivity class
import java.text.NumberFormat
import java.util.Locale
import androidx.lifecycle.LifecycleOwner // General lifecycle owner
import androidx.lifecycle.ViewModelProvider // If you were using it directly
import androidx.lifecycle.viewmodel.CreationExtras // If using ViewModelProvider.Factory

class CartFragment : Fragment(), CartItemListener { // Implement the listener interface

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to share CartViewModel with ProductDetailActivity and potentially others
    private val cartViewModel: CartViewModel = CartViewModel.instance

    private lateinit var cartAdapter: CartAdapter

    // Helper for currency formatting
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.buttonProceedToCheckout.setOnClickListener {
            if (cartViewModel.cartItems.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show()
            } else {
                // Navigate to your order/checkout activity
                // We decided to modify the existing 'order' Activity.
                // It needs to be adapted to handle a list of items from the cart.
                // For now, let's just navigate and show a Toast.
                // We'll discuss how to pass cart data to 'order' Activity next if this works.

                // Option A: Navigate to existing 'order' activity.
                // We need a way for 'order' activity to know it's coming from cart vs. buy now.
                // And to get the list of items or at least the subtotal.
                val intent = Intent(requireActivity(), OrderActivity::class.java)
                // How do we pass the cart items or subtotal?
                // For now, let's assume 'OrderActivity' activity can fetch from CartViewModel or we pass a flag
                intent.putExtra("FROM_CART", true)
                // We could also pass the subtotal directly if the 'order' activity is simple
                // intent.putExtra("CART_SUBTOTAL", cartViewModel.cartSubtotal.value)
                startActivity(intent)

                Toast.makeText(requireContext(), "Proceeding to checkout...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(this) // Pass 'this' as the listener
        binding.recyclerViewCartItems.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items -> // Corrected: viewLifecycleOwner is fine here if observeViewModel is called from onViewCreated. The issue might be elsewhere or an import.
            cartAdapter.submitList(items)
            binding.textViewEmptyCartMessage.isVisible = items.isNullOrEmpty()
            binding.recyclerViewCartItems.isVisible = !items.isNullOrEmpty()
            binding.buttonProceedToCheckout.isEnabled = !items.isNullOrEmpty()

            if (items.isNullOrEmpty()) {
                binding.textViewCartSubtotal.text = currencyFormatter.format(0.0)
            }
        }

        cartViewModel.cartSubtotal.observe(viewLifecycleOwner) { subtotal -> // Corrected
            binding.textViewCartSubtotal.text = currencyFormatter.format(subtotal)
        }
    }

    // Implementation of CartItemListener
    override fun onIncreaseQuantity(cartItem: CartItem) {
        cartViewModel.updateItemQuantity(cartItem.uniqueId, cartItem.quantity + 1)
    }

    override fun onDecreaseQuantity(cartItem: CartItem) {
        if (cartItem.quantity > 1) {
            cartViewModel.updateItemQuantity(cartItem.uniqueId, cartItem.quantity - 1)
        }
        // If quantity becomes 1, the adapter will disable the button.
        // If user tries to decrease from 1, nothing happens here.
        // Removal is handled by the remove button.
    }

    override fun onRemoveItem(cartItem: CartItem) {
        cartViewModel.removeItemFromCart(cartItem.uniqueId)
        Toast.makeText(requireContext(), "${cartItem.productName} removed from cart", Toast.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Crucial to prevent memory leaks with ViewBinding in Fragments
    }
}