package com.example.mytalabat.ui.orders

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mytalabat.data.model.Order
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.OrderRepository
import com.example.mytalabat.data.repository.UserRepository
import com.example.mytalabat.databinding.FragmentOrdersBinding
import com.example.mytalabat.util.Resource
import com.google.android.material.tabs.TabLayout

class OrdersFragment : Fragment() {
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private lateinit var ordersAdapter: OrdersAdapter
    private val viewModel: OrdersViewModel by viewModels {
        val dataSource = FirebaseDataSource()
        OrdersViewModelFactory(
            AuthRepository(dataSource),
            UserRepository(dataSource),
            OrderRepository(dataSource)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupTabs()
    }

    private fun setupUI() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshOrders()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadAvailableOrders() // Available Orders
                    1 -> viewModel.loadMyDeliveries()    // My Deliveries
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                refreshOrders()
            }
        })
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(
            onAcceptOrder = { order ->
                showAcceptOrderDialog(order)
            },
            onMarkDelivered = { order ->
                showMarkDeliveredDialog(order)
            }
        )
        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
        }
    }

    private fun setupObservers() {
        // Observe available orders
        viewModel.availableOrders.observe(viewLifecycleOwner) { resource ->
            // Only update UI if we're on Available tab
            if (binding.tabLayout.selectedTabPosition == 0) {
                handleOrdersResource(resource)
            }
        }

        // Observe my deliveries
        viewModel.myDeliveries.observe(viewLifecycleOwner) { resource ->
            // Only update UI if we're on My Deliveries tab
            if (binding.tabLayout.selectedTabPosition == 1) {
                handleOrdersResource(resource)
            }
        }

        // Observe accept order action
        viewModel.acceptOrderState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "✅ Order accepted! Moved to My Deliveries",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Switch to My Deliveries tab
                    binding.tabLayout.getTabAt(1)?.select()

                    // Refresh both tabs
                    viewModel.loadAvailableOrders()
                    viewModel.loadMyDeliveries()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Failed to accept order: ${resource.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Observe mark delivered action
        viewModel.markDeliveredState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "🎉 Delivery completed!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh My Deliveries tab
                    viewModel.loadMyDeliveries()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Failed to mark as delivered: ${resource.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Observe user profile
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { profile ->
                        Log.d("OrdersFragment", "User profile loaded: ${profile.name}")
                    }
                }
                is Resource.Error -> {
                    Log.e("OrdersFragment", "Error loading profile: ${resource.message}")
                }
                else -> {}
            }
        }
    }

    private fun handleOrdersResource(resource: Resource<List<Order>>) {
        binding.swipeRefresh.isRefreshing = false
        when (resource) {
            is Resource.Loading -> {
                showLoading(true)
                binding.tvEmptyOrders.visibility = View.GONE
            }
            is Resource.Success -> {
                showLoading(false)
                resource.data?.let { orders ->
                    Log.d("OrdersFragment", "Displaying ${orders.size} orders on tab ${binding.tabLayout.selectedTabPosition}")

                    if (orders.isEmpty()) {
                        binding.tvEmptyOrders.visibility = View.VISIBLE
                        binding.recyclerViewOrders.visibility = View.GONE
                        binding.tvEmptyOrders.text = if (binding.tabLayout.selectedTabPosition == 0) {
                            "No orders available for delivery"
                        } else {
                            "You have no active deliveries"
                        }
                    } else {
                        binding.tvEmptyOrders.visibility = View.GONE
                        binding.recyclerViewOrders.visibility = View.VISIBLE
                        ordersAdapter.submitList(orders)
                    }
                }
            }
            is Resource.Error -> {
                showLoading(false)
                binding.tvEmptyOrders.visibility = View.VISIBLE
                binding.tvEmptyOrders.text = "Error: ${resource.message}"
                Toast.makeText(
                    requireContext(),
                    "Error loading orders: ${resource.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showAcceptOrderDialog(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("Accept Delivery")
            .setMessage(
                "Accept delivery for order from ${order.shopName}?\n\n" +
                        "Delivery to: ${order.address}\n" +
                        "Total: ${order.totalPrice} EGP"
            )
            .setPositiveButton("Accept") { _, _ ->
                viewModel.acceptOrder(order.orderId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMarkDeliveredDialog(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("Mark as Delivered")
            .setMessage(
                "Confirm delivery completion for order from ${order.shopName}?"
            )
            .setPositiveButton("Delivered") { _, _ ->
                viewModel.markAsDelivered(order.orderId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshOrders() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> viewModel.loadAvailableOrders()
            1 -> viewModel.loadMyDeliveries()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}