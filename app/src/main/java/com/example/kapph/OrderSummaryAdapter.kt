package com.example.kapph // Or your preferred adapter package e.g., com.example.kapph.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kapph.ui.cart.CartItemDiffCallback
import java.text.NumberFormat
import java.util.Locale

class OrderSummaryAdapter :
    ListAdapter<CartItem, OrderSummaryAdapter.OrderSummaryViewHolder>(CartItemDiffCallback()) { // Reuse CartItemDiffCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderSummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_summary_product, parent, false) // Use your new item layout
        return OrderSummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderSummaryViewHolder, position: Int) {
        val cartItem = getItem(position)
        holder.bind(cartItem)
    }

    class OrderSummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.order_summary_item_image)
        private val itemName: TextView = itemView.findViewById(R.id.order_summary_item_name)
        private val itemDetails: TextView = itemView.findViewById(R.id.order_summary_item_details)
        private val itemTotalPrice: TextView = itemView.findViewById(R.id.order_summary_item_total_price_for_item)

        // Helper for currency formatting
        private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))

        fun bind(cartItem: CartItem) {
            itemName.text = cartItem.productName
            itemImage.setImageResource(cartItem.productImageResId)

            // Combine size and quantity for the details text view
            val detailsText = "Size: ${cartItem.selectedSize}, Qty: ${cartItem.quantity}"
            itemDetails.text = detailsText

            val totalPriceForItem = cartItem.pricePerUnit * cartItem.quantity
            itemTotalPrice.text = currencyFormatter.format(totalPriceForItem)
        }
    }
}

// We can reuse the CartItemDiffCallback from CartAdapter if it's accessible
// If not, you can copy it here or make it a top-level class in a common file.
// For simplicity, if CartAdapter.kt and this are in different packages and CartItemDiffCallback is
// internal to CartAdapter, you might need to redefine it or make it public / top-level.
// Assuming CartItemDiffCallback is defined as a public top-level class or accessible:

// class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
//    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
//        return oldItem.uniqueId == newItem.uniqueId
//    }
//
//    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
//        return oldItem == newItem
//    }
// }