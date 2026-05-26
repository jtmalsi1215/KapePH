package com.example.kapph.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // For sharing ViewModels if needed, or viewModels for fragment-scoped
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager // Or LinearLayoutManager
import com.example.kapph.Product
import com.example.kapph.ProductDetailActivity
import com.example.kapph.ProductViewModel // Assuming ProductViewModel holds all products
import com.example.kapph.R
import com.example.kapph.ui.favorites.FavoriteProductAdapter
import com.example.kapph.ui.favorites.FavoriteProductInteractionListener
import com.example.kapph.databinding.FragmentFavoritesBinding
import com.example.kapph.ui.cart.CartViewModel // For adding to cart
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class FavoritesFragment : Fragment(), FavoriteProductInteractionListener {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels if ProductViewModel needs to be shared across fragments in MainActivity
    // or if FavoritesViewModel state needs to persist with MainActivity scope.
    // If they are specific to this fragment's lifecycle, viewModels() is fine.
    // For favorites linked to a user, activityViewModels for FavoritesViewModel makes sense
    // so it persists across bottom nav changes. ProductViewModel likely also activityViewModels.
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels() // Assuming it holds all products
    private val cartViewModel: CartViewModel = CartViewModel.instance // Using singleton for cart

    private lateinit var favoriteProductAdapter: FavoriteProductAdapter

    private var allProductsList: List<Product> = emptyList()
    private var currentFavoriteIds: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModels()

        // Initial fetch if user might have logged in/out or data changed
        // This will be called again in onResume for robustness
        if (Firebase.auth.currentUser != null) {
            favoritesViewModel.fetchFavoriteProductIds()
        } else {
            // Clear UI if no user
            updateFavoritesDisplay(emptyList())
            binding.textViewNoFavorites.text = "Please login to see your favorites."
            binding.textViewNoFavorites.isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites when fragment becomes visible, in case of login/logout
        // or changes made elsewhere.
        if (Firebase.auth.currentUser != null) {
            favoritesViewModel.fetchFavoriteProductIds()
            binding.textViewNoFavorites.text = "You have no favorite items yet." // Reset default no fav text
        } else {
            updateFavoritesDisplay(emptyList())
            binding.textViewNoFavorites.text = "Please login to see your favorites."
            binding.textViewNoFavorites.isVisible = true
            binding.recyclerViewFavoriteProducts.isVisible = false
        }
    }


    private fun setupRecyclerView() {
        favoriteProductAdapter = FavoriteProductAdapter(this)
        binding.recyclerViewFavoriteProducts.apply {
            // Use GridLayoutManager if you want a grid, like your HomeFragment
            // For a simple list, LinearLayoutManager is fine. Let's use GridLayout for consistency.
            layoutManager = GridLayoutManager(requireContext(), 1) // 2 columns
            adapter = favoriteProductAdapter
            // setHasFixedSize(true) // If item sizes are fixed
        }
    }

    private fun observeViewModels() {
        favoritesViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarFavorites.isVisible = isLoading
            if (isLoading) { // Hide other views when loading
                binding.recyclerViewFavoriteProducts.isVisible = false
                binding.textViewNoFavorites.isVisible = false
            }
        }

        favoritesViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // You might want to clear the error in ViewModel after showing
            }
        }

        allProductsList = productViewModel.getAllProducts() // Get the static list
        Log.d("FavoritesFragment", "All products loaded initially: ${allProductsList.size} items")

        // Observer for favorite product IDs from FavoritesViewModel
        favoritesViewModel.favoriteProductIds.observe(viewLifecycleOwner, Observer { favoriteIds ->
            Log.d("FavoritesFragment", "Favorite IDs updated: ${favoriteIds.size} items")
            currentFavoriteIds = favoriteIds
            filterAndDisplayFavorites()
        })
    }

    private fun filterAndDisplayFavorites() {
        if (Firebase.auth.currentUser == null) {
            updateFavoritesDisplay(emptyList()) // Clear display if no user
            binding.textViewNoFavorites.text = "Please login to see your favorites."
            binding.textViewNoFavorites.isVisible = true
            return
        }

        if (allProductsList.isNotEmpty()) {
            val favProducts = allProductsList.filter { product ->
                currentFavoriteIds.contains(product.name) // Assuming product.name is your productId
            }
            Log.d("FavoritesFragment", "Filtered favorite products: ${favProducts.size} items")
            updateFavoritesDisplay(favProducts)
        } else if (currentFavoriteIds.isNotEmpty() && allProductsList.isEmpty()) {
            // We have favorite IDs, but the allProductsList isn't loaded yet.
            // This might happen if productViewModel loads slower.
            // The UI will update once allProductsList is populated.
            Log.d("FavoritesFragment", "Have favorite IDs but allProductsList is empty. Waiting for allProducts.")
            binding.progressBarFavorites.isVisible = true // Show loading as we are effectively waiting
        } else {
            // No favorite IDs or no products to filter from
            updateFavoritesDisplay(emptyList())
        }
    }

    private fun updateFavoritesDisplay(favoriteProductsList: List<Product>) {
        if (binding.progressBarFavorites.isVisible && favoriteProductsList.isNotEmpty()){
            binding.progressBarFavorites.isVisible = false // Hide progress if we now have items
        }

        favoriteProductAdapter.submitList(favoriteProductsList)
        binding.recyclerViewFavoriteProducts.isVisible = favoriteProductsList.isNotEmpty() && !binding.progressBarFavorites.isVisible
        binding.textViewNoFavorites.isVisible = favoriteProductsList.isEmpty() && !binding.progressBarFavorites.isVisible

        if (Firebase.auth.currentUser == null && favoriteProductsList.isEmpty()){
            binding.textViewNoFavorites.text = "Please login to see your favorites."
        } else if (favoriteProductsList.isEmpty() && !binding.progressBarFavorites.isVisible) {
            binding.textViewNoFavorites.text = "You have no favorite items yet."
        }

    }

    // --- FavoriteProductInteractionListener Implementation ---
    override fun onUnfavoriteClicked(product: Product) {
        if (Firebase.auth.currentUser == null) {
            Toast.makeText(requireContext(), "Please login to manage favorites.", Toast.LENGTH_SHORT).show()
            return
        }
        // Assuming product.name is the ID
        favoritesViewModel.removeFavorite(product.name)
        Toast.makeText(requireContext(), "${product.name} removed from favorites.", Toast.LENGTH_SHORT).show()
        // LiveData will update the list automatically
    }

    override fun onAddToCartClicked(product: Product) {
        // Here, we need to decide what "size" to add to cart from favorites.
        // Option 1: Add with a default size (e.g., "S").
        // Option 2: Navigate to ProductDetailActivity to let user choose size.
        // Let's go with Option 1 for simplicity now: Add with default size "S" and its base price.

        val basePrice = product.price.toDoubleOrNull() ?: 0.0 // Assuming product.price is base price for "S"
        if (basePrice == 0.0 && product.price != "0") {
            Log.e("FavoritesFragment", "Could not parse base price for ${product.name}")
            Toast.makeText(requireContext(), "Error getting price for ${product.name}", Toast.LENGTH_SHORT).show()
            return
        }

        cartViewModel.addItemToCart(product, "S", basePrice) // Assuming "S" and base price
        Toast.makeText(requireContext(), "${product.name} (S) added to cart.", Toast.LENGTH_SHORT).show()
    }

    override fun onItemClicked(product: Product) {
        // Navigate to ProductDetailActivity
        val intent = Intent(requireActivity(), ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_NAME", product.name)
            putExtra("PRODUCT_PRICE", product.price) // Pass original price string
            putExtra("PRODUCT_IMAGE", product.imageResId)
            putExtra("PRODUCT_DESCRIPTION", product.description)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}