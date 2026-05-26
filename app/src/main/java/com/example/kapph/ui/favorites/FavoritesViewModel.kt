package com.example.kapph.ui.favorites

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kapph.Product // Assuming your Product data class
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FavoritesViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // LiveData to hold the set of favorite product IDs for the current user
    private val _favoriteProductIds = MutableLiveData<Set<String>>(emptySet())
    val favoriteProductIds: LiveData<Set<String>> get() = _favoriteProductIds

    // LiveData to hold the actual Product objects that are favorites
    // This will be populated based on _favoriteProductIds and your product list source
    private val _favoriteProducts = MutableLiveData<List<Product>>(emptyList())
    val favoriteProducts: LiveData<List<Product>> get() = _favoriteProducts

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        fetchFavoriteProductIds()
    }

    fun fetchFavoriteProductIds() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _favoriteProductIds.value = emptySet() // No user, no favorites
            _errorMessage.value = "User not logged in."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Clear previous error

        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(userId)
                val document = userDocRef.get().await() // Using await for cleaner async

                if (document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val ids = document.get("favoriteProductIds") as? List<String>
                    _favoriteProductIds.value = ids?.toSet() ?: emptySet()
                    Log.d("FavoritesVM", "Fetched favorite IDs: ${_favoriteProductIds.value}")
                } else {
                    _favoriteProductIds.value = emptySet() // No document, no favorites yet
                    Log.d("FavoritesVM", "User document does not exist for favorites.")
                }
            } catch (e: Exception) {
                Log.e("FavoritesVM", "Error fetching favorite IDs", e)
                _errorMessage.value = "Error fetching favorites: ${e.message}"
                _favoriteProductIds.value = emptySet()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addFavorite(productId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "Cannot add favorite: User not logged in."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(userId)
                // Use FieldValue.arrayUnion to add productId if it doesn't already exist in the array
                userDocRef.update("favoriteProductIds", FieldValue.arrayUnion(productId))
                    .addOnSuccessListener {
                        // To ensure LiveData updates, refetch or update manually
                        val currentFavorites = _favoriteProductIds.value?.toMutableSet() ?: mutableSetOf()
                        currentFavorites.add(productId)
                        _favoriteProductIds.value = currentFavorites
                        Log.d("FavoritesVM", "$productId added to favorites")
                        _isLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesVM", "Error adding favorite", e)
                        _errorMessage.value = "Failed to add favorite: ${e.message}"
                        _isLoading.value = false
                    }
                // If the document or field doesn't exist, arrayUnion might fail or do nothing.
                // A more robust way is to check if doc exists, then either set or update.
                // For simplicity with arrayUnion, Firestore handles creating the array if it doesn't exist
                // when used with .set(mapOf(...), SetOptions.merge()) or if the document exists.
                // Let's ensure the document exists and has the field with .set and merge for robustness if it's the first favorite.
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userDocRef)
                    if (!snapshot.exists() || snapshot.get("favoriteProductIds") == null) {
                        // Document or field doesn't exist, create it with the new favorite
                        transaction.set(userDocRef, mapOf("favoriteProductIds" to listOf(productId)), SetOptions.merge())
                    } else {
                        // Field exists, just update (arrayUnion)
                        transaction.update(userDocRef, "favoriteProductIds", FieldValue.arrayUnion(productId))
                    }
                    null // Transaction must return a value or null
                }.addOnSuccessListener {
                    // Update LiveData
                    val currentFavorites = _favoriteProductIds.value?.toMutableSet() ?: mutableSetOf()
                    currentFavorites.add(productId)
                    _favoriteProductIds.value = currentFavorites
                    Log.d("FavoritesVM", "$productId added to favorites via transaction")
                }.addOnFailureListener { e ->
                    Log.e("FavoritesVM", "Error adding favorite via transaction", e)
                    _errorMessage.value = "Failed to add favorite: ${e.message}"
                }.await() // Using await to make it part of the coroutine scope for isLoading

            } catch (e: Exception) {
                Log.e("FavoritesVM", "Exception in addFavorite", e)
                _errorMessage.value = "Error adding favorite: ${e.message}"
            } finally {
                _isLoading.value = false // Ensure isLoading is reset
            }
        }
    }


    fun removeFavorite(productId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "Cannot remove favorite: User not logged in."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(userId)
                // Use FieldValue.arrayRemove to remove all instances of productId from the array
                userDocRef.update("favoriteProductIds", FieldValue.arrayRemove(productId))
                    .addOnSuccessListener {
                        val currentFavorites = _favoriteProductIds.value?.toMutableSet() ?: mutableSetOf()
                        currentFavorites.remove(productId)
                        _favoriteProductIds.value = currentFavorites
                        Log.d("FavoritesVM", "$productId removed from favorites")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesVM", "Error removing favorite", e)
                        _errorMessage.value = "Failed to remove favorite: ${e.message}"
                    }.await() // Make it part of coroutine for isLoading
            } catch (e: Exception) {
                Log.e("FavoritesVM", "Exception in removeFavorite", e)
                _errorMessage.value = "Error removing favorite: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isFavorite(productId: String): Boolean {
        return _favoriteProductIds.value?.contains(productId) ?: false
    }

    fun clearLocalFavoritesOnError() {
        _favoriteProductIds.value = emptySet()
        _favoriteProducts.value = emptyList()
    }

    // Call this function after _favoriteProductIds is updated
    // It requires a source of all products to filter from.
    // For now, let's assume you pass the full product list from where it's available (e.g., ProductViewModel)
    fun loadFavoriteProducts(allProducts: List<Product>) {
        val ids = _favoriteProductIds.value ?: return
        if (ids.isEmpty()) {
            _favoriteProducts.value = emptyList()
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            // In a real app, if products were also in Firestore, you might query them.
            // Here, we filter a local list.
            val favProducts = allProducts.filter { product ->
                ids.contains(product.name) // Assuming product.name is your productId
            }
            _favoriteProducts.value = favProducts
            _isLoading.value = false
            Log.d("FavoritesVM", "Loaded favorite products: ${favProducts.size}")
        }
    }
}