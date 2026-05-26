package com.example.kapph.ui.favorites // Or your chosen adapter package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kapph.Product
import com.example.kapph.databinding.ItemFavoriteProductBinding // ViewBinding class
import java.text.NumberFormat
import java.util.Locale

// Listener interface for adapter actions
interface FavoriteProductInteractionListener {
    fun onUnfavoriteClicked(product: Product)
    fun onAddToCartClicked(product: Product)
    fun onItemClicked(product: Product) // For navigating to product details
}

class FavoriteProductAdapter(
    private val listener: FavoriteProductInteractionListener
) : ListAdapter<Product, FavoriteProductAdapter.FavoriteProductViewHolder>(ProductDiffCallbackFav()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteProductViewHolder {
        val binding = ItemFavoriteProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteProductViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: FavoriteProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    class FavoriteProductViewHolder(
        private val binding: ItemFavoriteProductBinding,
        private val listener: FavoriteProductInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))

        fun bind(product: Product) {
            binding.favProductName.text = product.name
            binding.favProductDescription.text = product.description
            // Assuming product.price is a String like "100", convert to Double for formatter if needed
            // Or format directly if it's already suitable for display.
            // For now, let's assume it's a string that might need "P" prefix.
            // If your product.price already includes "P", just use it directly.
            val priceDouble = product.price.toDoubleOrNull()
            if (priceDouble != null) {
                binding.favProductPrice.text = currencyFormatter.format(priceDouble)
            } else {
                "P${product.price}".also { binding.favProductPrice.text = it } // Fallback or direct use
            }

            binding.favProductImage.setImageResource(product.imageResId)

            // The heart icon (fav_button_unfavorite) should always show as "favorited" (e.g., heart_clicked.xml)
            // as this list only contains favorites. Its click listener will trigger removal.
            // binding.favButtonUnfavorite.setImageResource(R.drawable.heart_clicked) // Already set in XML

            binding.favButtonUnfavorite.setOnClickListener {
                listener.onUnfavoriteClicked(product)
            }

            binding.favButtonAddToCart.setOnClickListener {
                listener.onAddToCartClicked(product)
            }

            // Click listener for the whole item
            binding.favItemConstraintLayout.setOnClickListener { // Or binding.root if card is root
                listener.onItemClicked(product)
            }
        }
    }
}

// DiffUtil for Product objects (can be shared if you have another Product adapter)
class ProductDiffCallbackFav : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        // Use a unique identifier for products if available, otherwise name.
        // If 'name' is not guaranteed unique, this might need adjustment.
        return oldItem.name == newItem.name // Assuming name is the unique ID for now
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem // Relies on Product being a data class
    }
}