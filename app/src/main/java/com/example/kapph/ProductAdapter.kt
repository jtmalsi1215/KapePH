package com.example.kapph

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil // Import for DiffUtil
import androidx.recyclerview.widget.ListAdapter // Import for ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

// Changed to ListAdapter for better performance with dynamic lists
class ProductAdapter(
    private val onItemClicked: (Product) -> Unit // Callback for item click
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) { // Use DiffUtil

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.img_caffe_mocha)
        private val productName: TextView = itemView.findViewById(R.id.caffe_mocha)
        private val productDescription: TextView = itemView.findViewById(R.id.product_category)
        private val productPrice: TextView = itemView.findViewById(R.id.product_price)
        // Plus button can still exist in layout, but we'll primarily use itemView click
        // val plusButton: ImageButton = itemView.findViewById(R.id.plus_button)

        private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))


        fun bind(product: Product, onItemClicked: (Product) -> Unit) {
            productImage.setImageResource(product.imageResId)
            productName.text = product.name
            productDescription.text = product.description

            // --- Price Handling ---
            var priceToFormat = product.price
            // Attempt to remove known currency symbols if they exist at the start
            if (priceToFormat.startsWith("P") || priceToFormat.startsWith("₱")) {
                priceToFormat = priceToFormat.substring(1) // Remove the first character
            }

            val priceDouble = priceToFormat.trim().toDoubleOrNull() // Trim any leading/trailing spaces after symbol removal

            if (priceDouble != null) {
                // We have a clean number, so use the formatter
                productPrice.text = currencyFormatter.format(priceDouble)
            } else {
                // Could not parse to a number after attempting to clean it.
                // Display the original product.price as a fallback, hoping it's already formatted.
                // Or, if product.price was something like "P100" initially, this will now be "100"
                // and if we want to ensure "P" is there if formatter fails:
                if (product.price.startsWith("P") || product.price.startsWith("₱")) {
                    productPrice.text = product.price // Original already had symbol
                } else {
                    productPrice.text = "P${product.price}" // Prepend P if original didn't have it and parsing failed
                }
                Log.w("ProductAdapter", "Could not parse price string '${product.price}' to Double. Displaying as is or with P prefix.")
            }
            // --- End Price Handling ---

            itemView.setOnClickListener {
                onItemClicked(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position) // getItem() is from ListAdapter
        holder.bind(product, onItemClicked)
    }

    // No need for getItemCount() when using ListAdapter

    // No need for updateProducts() method, ListAdapter handles updates with submitList()
}

// DiffUtil Callback for Products
class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        // Assuming product name is unique, or use a dedicated ID field if available
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem // Relies on Product being a data class
    }
}