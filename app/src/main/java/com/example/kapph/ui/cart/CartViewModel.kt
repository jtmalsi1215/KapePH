package com.example.kapph.ui.cart

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kapph.CartItem
import com.example.kapph.Product

class CartViewModel : ViewModel() {

    private val _cartItems = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cartItems: LiveData<List<CartItem>> get() = _cartItems as LiveData<List<CartItem>> // Cast for safety

    private val _cartSubtotal = MutableLiveData<Double>(0.0)
    val cartSubtotal: LiveData<Double> get() = _cartSubtotal

    private val _totalCartItemCount = MutableLiveData<Int>(0)
    val totalCartItemCount: LiveData<Int> get() = _totalCartItemCount

    // --- Add Companion Object for Singleton-like access ---
    companion object {
        // Lazy initialized instance
        val instance: CartViewModel by lazy {
            CartViewModel()
        }
    }
    // --- End Companion Object ---


    fun addItemToCart(product: Product, selectedSize: String, priceForSize: Double, quantityToAdd: Int = 1) {
        val currentList = _cartItems.value ?: mutableListOf() // Ensure it's mutable
        val productId = product.name
        val itemUniqueId = "${productId}_${selectedSize}"

        val existingItem = currentList.find { it.uniqueId == itemUniqueId }

        if (existingItem != null) {
            existingItem.quantity += quantityToAdd
        } else {
            val cartItem = CartItem(
                productId = productId,
                productName = product.name,
                productImageResId = product.imageResId,
                productDescription = product.description,
                selectedSize = selectedSize,
                quantity = quantityToAdd,
                pricePerUnit = priceForSize
            )
            currentList.add(cartItem)
        }
        _cartItems.value = ArrayList(currentList) // Force update by creating a new list instance
        recalculateTotals()
    }

    fun removeItemFromCart(cartItemUniqueId: String) {
        val currentList = _cartItems.value ?: mutableListOf()
        val newList = currentList.filterNot { it.uniqueId == cartItemUniqueId }.toMutableList()
        _cartItems.value = ArrayList(newList)
        recalculateTotals()
    }

    // In CartViewModel.kt
    fun updateItemQuantity(cartItemUniqueId: String, newQuantity: Int) {
        val currentList = _cartItems.value ?: mutableListOf()
        val updatedList = mutableListOf<CartItem>()
        var itemFoundAndUpdated = false

        Log.d("CartViewModel", "updateItemQuantity called for $cartItemUniqueId, newQuantity: $newQuantity")

        for (item in currentList) {
            if (item.uniqueId == cartItemUniqueId) {
                itemFoundAndUpdated = true
                if (newQuantity > 0) {
                    // Create a NEW CartItem instance with the updated quantity
                    updatedList.add(item.copy(quantity = newQuantity))
                    Log.d("CartViewModel", "Item ${item.productName} quantity updated to $newQuantity. New instance created.")
                } else {
                    Log.d("CartViewModel", "Item ${item.productName} being removed due to quantity <= 0.")
                    // Do not add it to updatedList, effectively removing it
                }
            } else {
                updatedList.add(item) // Add other items as they are
            }
        }

        if (itemFoundAndUpdated) {
            _cartItems.value = updatedList // Assign the new list with the (potentially) new CartItem instance
            Log.d("CartViewModel", "LiveData posted with updated list.")
        } else {
            Log.w("CartViewModel", "Item with uniqueId $cartItemUniqueId not found for update.")
        }
        recalculateTotals()
    }

    fun clearCart() {
        _cartItems.value = mutableListOf()
        recalculateTotals()
    }

    private fun recalculateTotals() {
        val items = _cartItems.value ?: listOf()
        var subtotal = 0.0
        var count = 0
        for (item in items) {
            subtotal += item.pricePerUnit * item.quantity
            count += item.quantity
        }
        _cartSubtotal.value = subtotal
        _totalCartItemCount.value = count
    }

    fun getCartItem(cartItemUniqueId: String): CartItem? {
        return _cartItems.value?.find { it.uniqueId == cartItemUniqueId }
    }
}