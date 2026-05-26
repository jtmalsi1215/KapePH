package com.example.kapph

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.kapph.ui.cart.CartViewModel
import com.example.kapph.ui.favorites.FavoritesViewModel // Import FavoritesViewModel
import com.google.firebase.auth.ktx.auth // Import Firebase Auth KTX
import com.google.firebase.ktx.Firebase // Import Firebase KTX

class ProductDetailActivity : AppCompatActivity() {

    private val cartViewModel: CartViewModel = CartViewModel.instance // Using singleton instance
    private val favoritesViewModel: FavoritesViewModel by viewModels() // ViewModel for this activity

    // Product details
    private var currentProductName: String? = null // Will serve as our productId for favorites
    private var currentProductPriceString: String? = null
    private var currentProductImageResId: Int = 0
    private var currentProductDescription: String? = null
    private var currentSelectedSize: String = "S"
    private var currentPriceForSize: Double = 0.0

    // Views
    private lateinit var productPriceTextView: TextView
    private lateinit var selectedSizeButton: Button
    private lateinit var heartButton: ImageButton // Make heartButton a class member

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)
        supportActionBar?.hide()

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // Initialize views that need to be class members
        productPriceTextView = findViewById(R.id.detail_product_price)
        heartButton = findViewById(R.id.heart_button) // Initialize here

        // Get product details from Intent
        currentProductName = intent.getStringExtra("PRODUCT_NAME")
        currentProductPriceString = intent.getStringExtra("PRODUCT_PRICE")
        currentProductImageResId = intent.getIntExtra("PRODUCT_IMAGE", 0)
        currentProductDescription = intent.getStringExtra("PRODUCT_DESCRIPTION")

        if (currentProductName == null || currentProductPriceString == null) {
            Toast.makeText(this, "Error: Product details missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup UI with product details
        val productImage: ImageView = findViewById(R.id.detail_product_image)
        val productNameTextView: TextView = findViewById(R.id.detail_product_name)
        val productDescriptionTextView: TextView = findViewById(R.id.detail_product_description)

        productImage.setImageResource(currentProductImageResId)
        productNameTextView.text = currentProductName
        productDescriptionTextView.text = currentProductDescription

        // Setup size selection
        val buttonSm: Button = findViewById(R.id.button_sm)
        val buttonMed: Button = findViewById(R.id.button_med)
        val buttonLarg: Button = findViewById(R.id.button_larg)
        val sizeButtons = listOf(buttonSm, buttonMed, buttonLarg)
        selectedSizeButton = buttonSm
        selectedSizeButton.isSelected = true
        currentSelectedSize = selectedSizeButton.text.toString()
        val basePrice = currentProductPriceString!!.toDoubleOrNull() ?: 0.0
        updatePriceDisplay(basePrice)

        sizeButtons.forEach { button ->
            button.setOnClickListener {
                selectedSizeButton.isSelected = false
                button.isSelected = true
                selectedSizeButton = button
                currentSelectedSize = button.text.toString()
                updatePriceDisplay(basePrice)
                Log.d("SizeSelection", "Selected: $currentSelectedSize, Price: $currentPriceForSize")
            }
        }

        // --- Favorites Logic ---
        // Initial state of heart button
        currentProductName?.let { prodId ->
            // No need to call updateHeartButtonState here as LiveData observer will handle it after fetch
        }

        // Observe changes to favorite product IDs from ViewModel
        favoritesViewModel.favoriteProductIds.observe(this) { favoriteIds ->
            currentProductName?.let { prodId ->
                val isFavorite = favoriteIds.contains(prodId)
                heartButton.isSelected = isFavorite
                Log.d("ProductDetail", "Favorites LiveData: $prodId isFavorite: $isFavorite")
            }
        }

        favoritesViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                favoritesViewModel.clearLocalFavoritesOnError() // Clear message after showing
            }
        }

        // Optional: Observe loading state for favorites
        // favoritesViewModel.isLoading.observe(this) { isLoading ->
        //    // Show/hide a progress indicator for favorite operations if desired
        // }

        heartButton.setOnClickListener {
            val prodId = currentProductName
            if (prodId == null) {
                Toast.makeText(this, "Product ID not available.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (Firebase.auth.currentUser == null) {
                Toast.makeText(this, "Please login to manage favorites.", Toast.LENGTH_SHORT).show()
                // Optionally navigate to LoginActivity
                // startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }

            // Toggle favorite state based on current ViewModel state (more reliable than button's isSelected)
            if (favoritesViewModel.isFavorite(prodId)) {
                favoritesViewModel.removeFavorite(prodId)
                Toast.makeText(applicationContext, "$currentProductName removed from Favorites", Toast.LENGTH_SHORT).show()
            } else {
                favoritesViewModel.addFavorite(prodId)
                Toast.makeText(applicationContext, "$currentProductName added to Favorites", Toast.LENGTH_SHORT).show()
            }
            // The LiveData observer for favoriteProductIds will update heartButton.isSelected
        }
        // --- End Favorites Logic ---

        // Back Button
        val backButton: ImageButton = findViewById(R.id.back_Button)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Add to Cart Button
        val addToCartButton: Button = findViewById(R.id.button_add_to_cart)
        addToCartButton.setOnClickListener {
            val pName = currentProductName
            if (pName != null && currentProductDescription != null && currentProductImageResId != 0) {
                val product = Product(
                    name = pName,
                    price = currentProductPriceString!!,
                    imageResId = currentProductImageResId,
                    description = currentProductDescription!!
                )
                cartViewModel.addItemToCart(product, currentSelectedSize, currentPriceForSize)
                Toast.makeText(this, "$pName ($currentSelectedSize) added to cart", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Could not add item to cart.", Toast.LENGTH_SHORT).show()
            }
        }

        // Buy Now Button
        val buyNowButton: Button = findViewById(R.id.button_buy_now)
        buyNowButton.setOnClickListener {
            val intent = Intent(this, OrderActivity::class.java)
            intent.putExtra("PRODUCT_NAME", currentProductName)
            intent.putExtra("PRODUCT_PRICE", currentPriceForSize.toInt())
            intent.putExtra("PRODUCT_IMAGE", currentProductImageResId)
            intent.putExtra("PRODUCT_SIZE", currentSelectedSize)
            startActivity(intent)
        }
    }

    private fun updatePriceDisplay(basePrice: Double) {
        currentPriceForSize = when (currentSelectedSize) {
            "S" -> basePrice
            "M" -> basePrice + 20.0
            "L" -> basePrice + 40.0
            else -> basePrice
        }
        "₱${currentPriceForSize.toInt()}".also { productPriceTextView.text = it }
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite status when activity resumes, especially if user logs in/out
        // or if favorites could be changed by another part of the app not using this ViewModel instance.
        // If using a shared ViewModel or singleton for FavoritesVM, this might be less critical
        // but still good for robustness if state can change externally.
        if (Firebase.auth.currentUser != null) {
            Log.d("ProductDetail", "onResume: User logged in, fetching favorites.")
            favoritesViewModel.fetchFavoriteProductIds() // Re-fetch from Firestore
        } else {
            Log.d("ProductDetail", "onResume: User not logged in, clearing local favorites state.")
            favoritesViewModel.clearLocalFavoritesOnError() // Clears the LiveData in ViewModel
            // The LiveData observer for favoriteProductIds will then update the heartButton
        }
    }
}