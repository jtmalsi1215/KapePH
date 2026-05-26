package com.example.kapph.ui.history // Or your chosen adapter package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kapph.OrderDetails // Your OrderDetails data class
import com.example.kapph.databinding.ItemOrderHistoryBinding // ViewBinding class
import java.text.NumberFormat
import java.util.Locale

// Listener for item clicks (e.g., to view order details later)
interface OrderHistoryInteractionListener {
    fun onOrderClicked(orderDetails: OrderDetails)
}

class OrderHistoryAdapter(
    private val listener: OrderHistoryInteractionListener
) : ListAdapter<OrderDetails, OrderHistoryAdapter.OrderHistoryViewHolder>(OrderDetailsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHistoryViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderHistoryViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: OrderHistoryViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    class OrderHistoryViewHolder(
        private val binding: ItemOrderHistoryBinding,
        private val listener: OrderHistoryInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))

        fun bind(orderDetails: OrderDetails) {
            binding.textViewOrderId.text = orderDetails.orderId.take(8) + "..." // Show partial ID or full
            binding.textViewOrderDate.text = orderDetails.getFormattedTimestamp()
            binding.textViewOrderTotal.text = currencyFormatter.format(orderDetails.grandTotal)

            // For status, we don't have it in OrderDetails yet.
            // You could add a 'status: String = "Pending"' field to OrderDetails data class.
            // For now, let's set a default or hide it.
            binding.textViewOrderStatus.text = "Completed" // Placeholder status
            // binding.textViewOrderStatus.visibility = View.GONE // Or hide if no status field

            binding.root.setOnClickListener {
                listener.onOrderClicked(orderDetails)
            }
        }
    }
}

class OrderDetailsDiffCallback : DiffUtil.ItemCallback<OrderDetails>() {
    override fun areItemsTheSame(oldItem: OrderDetails, newItem: OrderDetails): Boolean {
        return oldItem.orderId == newItem.orderId
    }

    override fun areContentsTheSame(oldItem: OrderDetails, newItem: OrderDetails): Boolean {
        return oldItem == newItem // Relies on OrderDetails being a data class
    }
}