package com.example.kapph.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kapph.CartItem
import com.example.kapph.R // Ensure this R import is correct for your project
import java.text.NumberFormat
import java.util.Locale

// Define an interface for click listeners
interface CartItemListener {
    fun onIncreaseQuantity(cartItem: CartItem)
    fun onDecreaseQuantity(cartItem: CartItem)
    fun onRemoveItem(cartItem: CartItem)
}

class CartAdapter(private val listener: CartItemListener) :
    ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false) // Use your item_cart.xml
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = getItem(position)
        holder.bind(cartItem, listener)
    }

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.cart_item_image)
        private val itemName: TextView = itemView.findViewById(R.id.cart_item_name)
        private val itemSize: TextView = itemView.findViewById(R.id.cart_item_size)
        private val itemPricePerUnit: TextView = itemView.findViewById(R.id.cart_item_price_per_unit)
        private val itemQuantity: TextView = itemView.findViewById(R.id.cart_item_quantity)
        private val itemTotalPrice: TextView = itemView.findViewById(R.id.cart_item_total_price)
        private val increaseQuantityButton: ImageButton = itemView.findViewById(R.id.cart_item_increase_quantity)
        private val decreaseQuantityButton: ImageButton = itemView.findViewById(R.id.cart_item_decrease_quantity)
        private val removeItemButton: ImageButton = itemView.findViewById(R.id.cart_item_remove_button)

        // Helper for currency formatting (e.g., ₱100.00)
        private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH")) // For Philippine Peso "₱"

        fun bind(cartItem: CartItem, listener: CartItemListener) {
            itemName.text = cartItem.productName
            itemImage.setImageResource(cartItem.productImageResId)
            "Size: ${cartItem.selectedSize}".also { itemSize.text = it }
            "${currencyFormatter.format(cartItem.pricePerUnit)} / unit".also { itemPricePerUnit.text = it }
            cartItem.quantity.toString().also { itemQuantity.text = it }

            val totalPriceForItem = cartItem.pricePerUnit * cartItem.quantity
            itemTotalPrice.text = currencyFormatter.format(totalPriceForItem)

            // Set click listeners
            increaseQuantityButton.setOnClickListener {
                listener.onIncreaseQuantity(cartItem)
            }

            decreaseQuantityButton.setOnClickListener {
                // Only trigger listener if quantity > 1, though ViewModel also handles this
                if (cartItem.quantity > 1) {
                    listener.onDecreaseQuantity(cartItem)
                }
            }

            removeItemButton.setOnClickListener {
                listener.onRemoveItem(cartItem)
            }

            // Visually indicate and enable/disable decrease button
            if (cartItem.quantity > 1) {
                decreaseQuantityButton.isEnabled = true
                decreaseQuantityButton.alpha = 1.0f // Fully opaque (enabled)
            } else {
                decreaseQuantityButton.isEnabled = false
                decreaseQuantityButton.alpha = 0.5f // Semi-transparent (disabled) - adjust as needed
            }
        }
    }
}

// DiffUtil helps RecyclerView to efficiently update items
class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        // Check if the items represent the same object (e.g., by unique ID)
        return oldItem.uniqueId == newItem.uniqueId
    }

    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        // Check if the data within the item has changed
        return oldItem == newItem // Data class equality check works well here
    }
}