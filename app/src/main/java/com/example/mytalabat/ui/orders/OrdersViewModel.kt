package com.example.mytalabat.ui.orders

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytalabat.data.model.Order
import com.example.mytalabat.data.model.UserProfile
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.OrderRepository
import com.example.mytalabat.data.repository.UserRepository
import com.example.mytalabat.util.Resource
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<Resource<UserProfile>>()
    val userProfile: LiveData<Resource<UserProfile>> = _userProfile

    private val _availableOrders = MutableLiveData<Resource<List<Order>>>()
    val availableOrders: LiveData<Resource<List<Order>>> = _availableOrders

    private val _myDeliveries = MutableLiveData<Resource<List<Order>>>()
    val myDeliveries: LiveData<Resource<List<Order>>> = _myDeliveries

    private val _acceptOrderState = MutableLiveData<Resource<Unit>>()
    val acceptOrderState: LiveData<Resource<Unit>> = _acceptOrderState

    private val _markDeliveredState = MutableLiveData<Resource<Unit>>()
    val markDeliveredState: LiveData<Resource<Unit>> = _markDeliveredState

    init {
        loadUserProfile()
        loadAvailableOrders()
    }

    private fun loadUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _userProfile.value = Resource.Error("User not authenticated")
            return
        }

        _userProfile.value = Resource.Loading()
        viewModelScope.launch {
            val result = userRepository.getUserProfile(uid)
            _userProfile.value = result
        }
    }

    fun loadAvailableOrders() {
        Log.d("OrdersViewModel", "Loading available orders...")
        _availableOrders.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = orderRepository.getDeliveringOrders()
                Log.d("OrdersViewModel", "Result: ${result}")
                when (result) {
                    is Resource.Success -> {
                        // FIXED: Show ONLY orders with empty deliveryPersonId
                        val filtered = result.data?.filter { order ->
                            order.deliveryPersonId.isEmpty()
                        } ?: emptyList()

                        Log.d("OrdersViewModel", "Available orders (unassigned): ${filtered.size}")
                        _availableOrders.value = Resource.Success(filtered)
                    }
                    is Resource.Error -> {
                        _availableOrders.value = result
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("OrdersViewModel", "Error loading available orders", e)
                _availableOrders.value = Resource.Error(e.message ?: "Failed to load orders")
            }
        }
    }

    fun loadMyDeliveries() {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            _myDeliveries.value = Resource.Error("User not authenticated")
            return
        }

        Log.d("OrdersViewModel", "Loading my deliveries for userId: $userId")
        _myDeliveries.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = orderRepository.getMyDeliveries(userId)
                when (result) {
                    is Resource.Success -> {
                        Log.d("OrdersViewModel", "My deliveries count: ${result.data?.size}")
                        _myDeliveries.value = result
                    }
                    is Resource.Error -> {
                        _myDeliveries.value = result
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("OrdersViewModel", "Error loading my deliveries", e)
                _myDeliveries.value = Resource.Error(e.message ?: "Failed to load deliveries")
            }
        }
    }

    fun acceptOrder(orderId: String) {
        val userId = authRepository.getCurrentUser()?.uid
        val userName = _userProfile.value?.data?.name

        if (userId == null || userName == null) {
            _acceptOrderState.value = Resource.Error("User not authenticated")
            return
        }

        Log.d("OrdersViewModel", "Accepting order: $orderId for user: $userId ($userName)")
        _acceptOrderState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = orderRepository.acceptOrder(orderId, userId, userName)
                _acceptOrderState.value = result
            } catch (e: Exception) {
                Log.e("OrdersViewModel", "Error accepting order", e)
                _acceptOrderState.value = Resource.Error(e.message ?: "Failed to accept order")
            }
        }
    }

    fun markAsDelivered(orderId: String) {
        Log.d("OrdersViewModel", "Marking order as delivered: $orderId")
        _markDeliveredState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = orderRepository.markAsDelivered(orderId)
                _markDeliveredState.value = result
            } catch (e: Exception) {
                Log.e("OrdersViewModel", "Error marking as delivered", e)
                _markDeliveredState.value = Resource.Error(e.message ?: "Failed to mark as delivered")
            }
        }
    }
}