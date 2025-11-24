package com.example.mytalabat.data.repository

import android.util.Log
import com.example.mytalabat.data.model.Order
import com.example.mytalabat.data.model.DeliveryStats
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.util.Resource

class OrderRepository(private val dataSource: FirebaseDataSource) {

    suspend fun getDeliveringOrders(): Resource<List<Order>> {
        return try {
            val orders = dataSource.getDeliveringOrders()
            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error fetching delivering orders", e)
            Resource.Error(e.message ?: "Failed to fetch orders")
        }
    }

    suspend fun getMyDeliveries(deliveryPersonId: String): Resource<List<Order>> {
        return try {
            val orders = dataSource.getMyDeliveries(deliveryPersonId)
            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error fetching my deliveries", e)
            Resource.Error(e.message ?: "Failed to fetch deliveries")
        }
    }

    suspend fun acceptOrder(orderId: String, deliveryPersonId: String, deliveryPersonName: String): Resource<Unit> {
        return try {
            dataSource.acceptOrder(orderId, deliveryPersonId, deliveryPersonName)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error accepting order", e)
            Resource.Error(e.message ?: "Failed to accept order")
        }
    }

    suspend fun markAsDelivered(orderId: String): Resource<Unit> {
        return try {
            // Get current user ID for reward tracking
            val userId = dataSource.getCurrentUser()?.uid
            if (userId != null) {
                dataSource.markOrderAsDelivered(orderId, userId)
            } else {
                throw Exception("User not authenticated")
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error marking order as delivered", e)
            Resource.Error(e.message ?: "Failed to mark order as delivered")
        }
    }

    // NEW: Get delivery stats
    suspend fun getDeliveryStats(deliveryPersonId: String): Resource<DeliveryStats> {
        return try {
            val stats = dataSource.getDeliveryStats(deliveryPersonId)
            if (stats != null) {
                Resource.Success(stats)
            } else {
                // Return empty stats if none exist
                Resource.Success(DeliveryStats(userId = deliveryPersonId))
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error fetching delivery stats", e)
            Resource.Error(e.message ?: "Failed to fetch stats")
        }
    }

    // NEW: Redeem points
    suspend fun redeemPoints(deliveryPersonId: String, pointsToRedeem: Int): Resource<Boolean> {
        return try {
            val success = dataSource.redeemPoints(deliveryPersonId, pointsToRedeem)
            if (success) {
                Resource.Success(true)
            } else {
                Resource.Error("Insufficient points or redemption failed")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error redeeming points", e)
            Resource.Error(e.message ?: "Failed to redeem points")
        }
    }
}