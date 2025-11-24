package com.example.mytalabat.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mytalabat.data.model.Order
import com.example.mytalabat.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(
    private val onAcceptOrder: (Order) -> Unit,
    private val onMarkDelivered: (Order) -> Unit
) : ListAdapter<Order, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Order ID
            binding.tvOrderId.text = "Order: ${order.orderId.take(8)}..."

            // Shop name
            binding.tvShopName.text = "From: ${order.shopName}"

            // Buyer name
            binding.tvBuyerName.text = "Customer: ${order.buyerName}"

            // Delivery address
            binding.tvAddress.text = "📍 ${order.address}"

            // Delivery zone
            binding.tvZone.text = "Zone: ${order.deliveryZone}"

            // Total price
            binding.tvTotalPrice.text = "${order.totalPrice} EGP"

            // Payment method
            binding.tvPaymentMethod.text = "Payment: ${order.paymentMethod}"

            // Items list
            val itemsText = order.items.joinToString("\n") { item ->
                "• ${item.productName} x${item.quantity}"
            }
            binding.tvItems.text = itemsText

            // Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(order.createdAt))

            // Show/hide buttons based on order state
            if (order.deliveryPersonId.isEmpty()) {
                // Order not accepted yet - show Accept button
                binding.btnAccept.visibility = View.VISIBLE
                binding.btnMarkDelivered.visibility = View.GONE
                binding.tvDeliveryPerson.visibility = View.GONE

                binding.btnAccept.setOnClickListener {
                    onAcceptOrder(order)
                }
            } else {
                // Order already accepted - show Mark Delivered button
                binding.btnAccept.visibility = View.GONE
                binding.btnMarkDelivered.visibility = View.VISIBLE
                binding.tvDeliveryPerson.visibility = View.VISIBLE
                binding.tvDeliveryPerson.text = "Assigned to: ${order.deliveryPersonName}"

                binding.btnMarkDelivered.setOnClickListener {
                    onMarkDelivered(order)
                }
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}