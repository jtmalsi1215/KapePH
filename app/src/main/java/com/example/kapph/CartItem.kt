package com.example.kapph

// It's good practice to place data classes that are used across different
// parts of your app (like Product, and now CartItem) in a common package,
// or a dedicated 'data' or 'model' package.
// For now, placing it in com.example.kapph is fine.

data class CartItem(
    val productId: String, // Using product name as a simple ID for now
    val productName: String,
    val productImageResId: Int,
    val productDescription: String, // Or category
    val selectedSize: String,
    var quantity: Int,
    val pricePerUnit: Double // Store the price for this item (considering size)
) {
    // It might be useful to have a unique identifier for a cart item if you
    // want to easily find/update/remove it.
    // This could be a combination of productId and selectedSize.
    val uniqueId: String
        get() = "${productId}_${selectedSize}"
}