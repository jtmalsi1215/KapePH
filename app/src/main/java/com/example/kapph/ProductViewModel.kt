package com.example.kapph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Locale // Added for case-insensitive search

class ProductViewModel : ViewModel() {

    // Holds the original, unmodified list of all products
    private val allProductsList: List<Product> = listOf(
        Product("Caffe Mocha", "100", R.drawable.caffee_mocha, "Deep Foam"),
        Product("Flat White", "150", R.drawable.flat_white, "Espresso"),
        Product("Mocha Fusi", "60", R.drawable.mocha_fuzzi, "Ice/Hot"),
        Product("Cafe Panna", "60", R.drawable.caffee_panna, "Espresso"),
        Product("Cafe Latte", "120", R.drawable.caffee_mocha, "Deep Foam"),
        Product("Affogato", "200", R.drawable.flat_white, "Espresso")
        // Add your other products back if needed
    )

    // LiveData for the products to be displayed (can be all products or filtered products)
    private val _displayedProducts = MutableLiveData<List<Product>>()
    val displayedProducts: LiveData<List<Product>> get() = _displayedProducts // Changed name for clarity

    // LiveData to indicate if the current list is empty due to no search results
    private val _noSearchResults = MutableLiveData<Boolean>(false)
    val noSearchResults: LiveData<Boolean> get() = _noSearchResults

    init {
        // Initially, display all products
        _displayedProducts.value = allProductsList
    }

    fun searchProducts(query: String?) {
        if (query.isNullOrBlank()) {
            // If query is empty, show all products
            _displayedProducts.value = allProductsList
            _noSearchResults.value = false
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            val filteredList = allProductsList.filter { product ->
                // Search in product name and description (case-insensitive)
                product.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                        product.description.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            }
            _displayedProducts.value = filteredList
            _noSearchResults.value = filteredList.isEmpty()
        }
    }

    // If you still need a direct LiveData for all products (e.g., for FavoritesFragment)
    // you can keep it or have FavoritesFragment get it from `allProductsList` via a getter method.
    // For simplicity, let's assume HomeFragment will now observe `displayedProducts`.
    // If other parts of the app need the full, unfiltered list, this could be exposed differently.
    // For example, a simple getter:
    fun getAllProducts(): List<Product> {
        return allProductsList
    }
}