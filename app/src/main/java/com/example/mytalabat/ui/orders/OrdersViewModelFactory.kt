package com.example.mytalabat.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.OrderRepository
import com.example.mytalabat.data.repository.UserRepository

class OrdersViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrdersViewModel(authRepository, userRepository, orderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}