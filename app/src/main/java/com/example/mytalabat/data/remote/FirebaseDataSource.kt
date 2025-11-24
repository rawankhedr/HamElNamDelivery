package com.example.mytalabat.data.remote

import android.util.Log
import com.example.mytalabat.data.model.User
import com.example.mytalabat.data.model.UserProfile
import com.example.mytalabat.data.model.Order
import com.example.mytalabat.data.model.OrderItem
import com.example.mytalabat.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import com.example.mytalabat.data.model.DeliveryStats  // ADD THIS

class FirebaseDataSource {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(
            "https://mytalabat2-default-rtdb.europe-west1.firebasedatabase.app"
        )
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Authentication failed")
        return User(firebaseUser.uid, firebaseUser.email ?: "")
    }

    suspend fun signUp(email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Registration failed")
        return User(firebaseUser.uid, firebaseUser.email ?: "")
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        Log.d("FirebaseDataSource", "Saving profile: $profile")
        database.getReference(Constants.PROFILES_REF)
            .child(profile.uid)
            .setValue(profile.toMap())
            .await()
        Log.d("FirebaseDataSource", "Profile saved successfully")
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = database.getReference(Constants.PROFILES_REF).child(uid).get().await()
        if (!snapshot.exists()) return null
        return try {
            UserProfile(
                uid = snapshot.child("uid").getValue(String::class.java) ?: uid,
                name = snapshot.child("name").getValue(String::class.java) ?: "",
                email = snapshot.child("email").getValue(String::class.java) ?: "",
                phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: "",
                profilePhotoUrl = snapshot.child("profilePhotoUrl").getValue(String::class.java) ?: "",
                isSeller = snapshot.child("isSeller").getValue(Boolean::class.java) ?: false,
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            ).also {
                Log.d("FirebaseDataSource", "Fetched profile: $it")
            }
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "Error parsing profile: ${e.message}")
            null
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        database.getReference(Constants.PROFILES_REF)
            .child(uid)
            .updateChildren(updates)
            .await()
    }

    // ===== NEW: ORDER FUNCTIONS FOR DELIVERY APP =====

    /**
     * Get all orders with status "delivering"
     */
    suspend fun getDeliveringOrders(): List<Order> {
        val ref = database.getReference(Constants.ORDERS_REF)
            .orderByChild("status")
            .equalTo("delivering")

        val snapshot = ref.get().await()
        val orders = mutableListOf<Order>()

        for (childSnapshot in snapshot.children) {
            try {
                val order = parseOrderFromSnapshot(childSnapshot)
                orders.add(order)
            } catch (e: Exception) {
                Log.e("FirebaseDataSource", "Error parsing order: ${e.message}")
            }
        }

        Log.d("FirebaseDataSource", "Found ${orders.size} delivering orders")
        return orders
    }

    /**
     * Accept an order by setting deliveryPersonId
     */
    suspend fun acceptOrder(orderId: String, deliveryPersonId: String, deliveryPersonName: String) {
        val updates = mapOf(
            "deliveryPersonId" to deliveryPersonId,
            "deliveryPersonName" to deliveryPersonName
        )
        database.getReference(Constants.ORDERS_REF)
            .child(orderId)
            .updateChildren(updates)
            .await()
        Log.d("FirebaseDataSource", "Order $orderId accepted by $deliveryPersonName")
    }

    /**
     * Mark order as delivered
     */
    suspend fun markOrderAsDelivered(orderId: String) {
        database.getReference(Constants.ORDERS_REF)
            .child(orderId)
            .child("status")
            .setValue("delivered")
            .await()
        Log.d("FirebaseDataSource", "Order $orderId marked as delivered")
    }

    /**
     * Get orders assigned to a specific delivery person
     */
    suspend fun getMyDeliveries(deliveryPersonId: String): List<Order> {
        val ref = database.getReference(Constants.ORDERS_REF)
            .orderByChild("deliveryPersonId")
            .equalTo(deliveryPersonId)

        val snapshot = ref.get().await()
        val orders = mutableListOf<Order>()

        for (childSnapshot in snapshot.children) {
            try {
                val order = parseOrderFromSnapshot(childSnapshot)
                // Only include orders that are still delivering
                if (order.status == "delivering") {
                    orders.add(order)
                }
            } catch (e: Exception) {
                Log.e("FirebaseDataSource", "Error parsing order: ${e.message}")
            }
        }

        return orders
    }

    private fun parseOrderFromSnapshot(snapshot: com.google.firebase.database.DataSnapshot): Order {
        val orderId = snapshot.key ?: ""

        // Parse items
        val itemsSnapshot = snapshot.child("items")
        val items = mutableListOf<OrderItem>()
        for (itemSnap in itemsSnapshot.children) {
            items.add(
                OrderItem(
                    productId = itemSnap.child("productId").getValue(String::class.java) ?: "",
                    productName = itemSnap.child("productName").getValue(String::class.java) ?: "",
                    productPrice = itemSnap.child("productPrice").getValue(Double::class.java) ?: 0.0,
                    quantity = itemSnap.child("quantity").getValue(Int::class.java) ?: 0,
                    subtotal = itemSnap.child("subtotal").getValue(Double::class.java) ?: 0.0
                )
            )
        }

        return Order(
            orderId = orderId,
            buyerId = snapshot.child("buyerId").getValue(String::class.java) ?: "",
            buyerName = snapshot.child("buyerName").getValue(String::class.java) ?: "",
            shopId = snapshot.child("shopId").getValue(String::class.java) ?: "",
            shopName = snapshot.child("shopName").getValue(String::class.java) ?: "",
            items = items,
            subtotal = snapshot.child("subtotal").getValue(Double::class.java) ?: 0.0,
            deliveryFee = snapshot.child("deliveryFee").getValue(Double::class.java) ?: 0.0,
            totalPrice = snapshot.child("totalPrice").getValue(Double::class.java) ?: 0.0,
            status = snapshot.child("status").getValue(String::class.java) ?: "",
            paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "",
            address = snapshot.child("address").getValue(String::class.java) ?: "",
            deliveryZone = snapshot.child("deliveryZone").getValue(String::class.java) ?: "",
            deliveryId = snapshot.child("deliveryId").getValue(String::class.java) ?: "",
            deliveryPersonId = snapshot.child("deliveryPersonId").getValue(String::class.java) ?: "",
            deliveryPersonName = snapshot.child("deliveryPersonName").getValue(String::class.java) ?: "",
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
        )
    }

    fun signOut() {
        auth.signOut()
    }
    // ===== REWARD SYSTEM FUNCTIONS =====

    /**
     * Mark order as delivered and award points
     */
    suspend fun markOrderAsDelivered(orderId: String, deliveryPersonId: String) {
        val deliveredAt = System.currentTimeMillis()

        // Get the order to calculate points
        val orderSnapshot = database.getReference(Constants.ORDERS_REF)
            .child(orderId)
            .get()
            .await()

        if (!orderSnapshot.exists()) {
            throw Exception("Order not found")
        }

        val totalPrice = orderSnapshot.child("totalPrice").getValue(Double::class.java) ?: 0.0

        // Calculate reward points
        val rewardPoints = calculateRewardPoints(totalPrice)

        // Update order status
        val updates = mapOf(
            "status" to "delivered",
            "deliveredAt" to deliveredAt,
            "rewardPoints" to rewardPoints
        )

        database.getReference(Constants.ORDERS_REF)
            .child(orderId)
            .updateChildren(updates)
            .await()

        // Update delivery person's stats if ID provided
        if (deliveryPersonId.isNotEmpty()) {
            updateDeliveryStats(deliveryPersonId, rewardPoints)
        }

        Log.d("FirebaseDataSource", "Order $orderId marked as delivered, awarded $rewardPoints points")
    }

    /**
     * Calculate reward points based on order value
     */
    private fun calculateRewardPoints(totalPrice: Double): Int {
        var points = Constants.POINTS_PER_DELIVERY

        // Bonus points for large orders
        if (totalPrice >= Constants.BONUS_POINTS_THRESHOLD) {
            points += Constants.BONUS_POINTS
        }

        return points
    }

    /**
     * Update delivery person's stats
     */
    private suspend fun updateDeliveryStats(deliveryPersonId: String, pointsEarned: Int) {
        val statsRef = database.getReference(Constants.DELIVERY_STATS_REF)
            .child(deliveryPersonId)

        val currentStats = statsRef.get().await()

        val stats = if (currentStats.exists()) {
            DeliveryStats(
                userId = deliveryPersonId,
                totalPoints = (currentStats.child("totalPoints").getValue(Int::class.java) ?: 0) + pointsEarned,
                totalDeliveries = (currentStats.child("totalDeliveries").getValue(Int::class.java) ?: 0) + 1,
                availablePoints = (currentStats.child("availablePoints").getValue(Int::class.java) ?: 0) + pointsEarned,
                redeemedPoints = currentStats.child("redeemedPoints").getValue(Int::class.java) ?: 0,
                totalEarnings = (currentStats.child("totalEarnings").getValue(Double::class.java) ?: 0.0) + (pointsEarned * Constants.EGP_PER_POINT),
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            DeliveryStats(
                userId = deliveryPersonId,
                totalPoints = pointsEarned,
                totalDeliveries = 1,
                availablePoints = pointsEarned,
                redeemedPoints = 0,
                totalEarnings = pointsEarned * Constants.EGP_PER_POINT,
                lastUpdated = System.currentTimeMillis()
            )
        }

        statsRef.setValue(stats.toMap()).await()
    }

    /**
     * Get delivery person's stats
     */
    suspend fun getDeliveryStats(deliveryPersonId: String): DeliveryStats? {
        val snapshot = database.getReference(Constants.DELIVERY_STATS_REF)
            .child(deliveryPersonId)
            .get()
            .await()

        if (!snapshot.exists()) return null

        return try {
            DeliveryStats(
                userId = deliveryPersonId,
                totalPoints = snapshot.child("totalPoints").getValue(Int::class.java) ?: 0,
                totalDeliveries = snapshot.child("totalDeliveries").getValue(Int::class.java) ?: 0,
                availablePoints = snapshot.child("availablePoints").getValue(Int::class.java) ?: 0,
                redeemedPoints = snapshot.child("redeemedPoints").getValue(Int::class.java) ?: 0,
                totalEarnings = snapshot.child("totalEarnings").getValue(Double::class.java) ?: 0.0,
                lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L
            )
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "Error parsing delivery stats: ${e.message}")
            null
        }
    }

    /**
     * Redeem points (convert to discount) - Used by BUYER app
     */
    suspend fun redeemPoints(deliveryPersonId: String, pointsToRedeem: Int): Boolean {
        val statsRef = database.getReference(Constants.DELIVERY_STATS_REF)
            .child(deliveryPersonId)

        val currentStats = statsRef.get().await()

        if (!currentStats.exists()) return false

        val availablePoints = currentStats.child("availablePoints").getValue(Int::class.java) ?: 0

        if (availablePoints < pointsToRedeem) return false

        val updates = mapOf(
            "availablePoints" to (availablePoints - pointsToRedeem),
            "redeemedPoints" to ((currentStats.child("redeemedPoints").getValue(Int::class.java) ?: 0) + pointsToRedeem),
            "lastUpdated" to System.currentTimeMillis()
        )

        statsRef.updateChildren(updates).await()
        return true
    }
}