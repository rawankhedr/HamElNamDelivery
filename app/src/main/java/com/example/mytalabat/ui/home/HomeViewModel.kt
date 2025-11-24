package com.example.mytalabat.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytalabat.data.model.DeliveryStats
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.OrderRepository
import com.example.mytalabat.util.Resource
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _deliveryStats = MutableLiveData<Resource<DeliveryStats>>()
    val deliveryStats: LiveData<Resource<DeliveryStats>> = _deliveryStats

    init {
        loadDeliveryStats()
    }

    fun loadDeliveryStats() {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _deliveryStats.value = Resource.Error("User not authenticated")
            return
        }

        _deliveryStats.value = Resource.Loading()
        viewModelScope.launch {
            val result = orderRepository.getDeliveryStats(uid)
            _deliveryStats.value = result
        }
    }

    fun refreshStats() {
        loadDeliveryStats()
    }
}