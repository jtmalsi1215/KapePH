package com.example.kapph.ui.history // Or your chosen package

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kapph.OrderDetails // Your data class
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderHistoryViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _orders = MutableLiveData<List<OrderDetails>>()
    val orders: LiveData<List<OrderDetails>> get() = _orders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchOrderHistory() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in. Cannot fetch order history."
            _orders.value = emptyList()
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val ordersSnapshot = db.collection("users").document(userId)
                    .collection("orders")
                    .orderBy("orderTimestamp", Query.Direction.DESCENDING) // Show newest first
                    .get()
                    .await()

                val fetchedOrders = ordersSnapshot.toObjects<OrderDetails>()
                _orders.value = fetchedOrders
                Log.d("OrderHistoryVM", "Fetched ${fetchedOrders.size} orders.")

            } catch (e: Exception) {
                Log.e("OrderHistoryVM", "Error fetching order history", e)
                _errorMessage.value = "Failed to load order history: ${e.message}"
                _orders.value = emptyList() // Clear orders on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}