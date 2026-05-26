package com.example.kapph // Or your data/model package

import com.google.firebase.firestore.ServerTimestamp // Import for @ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Add default values to all properties for Firestore deserialization
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val selectedSize: String = "",
    val quantity: Int = 0,
    val pricePerUnit: Double = 0.0
)

// Add default values to all properties for Firestore deserialization
data class OrderDetails(
    val orderId: String = UUID.randomUUID().toString(),
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val deliveryMethod: String = "",
    val deliveryAddress: String? = null,
    @ServerTimestamp val orderTimestamp: Date? = null,
    val notes: String? = null
) {
    fun getFormattedTimestamp(): String {
        return orderTimestamp?.let {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(it) // 'it' is already a Date
        } ?: "N/A"
    }
    // No-argument constructor is still effectively provided by Kotlin due to defaults
}