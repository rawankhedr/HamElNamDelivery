package com.example.mytalabat.data.model

/**
 * Represents a complete order with all its details
 */
data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalPrice: Double = 0.0,
    val status: String = "", // "delivering", "delivered"
    val paymentMethod: String = "",
    val address: String = "",
    val deliveryZone: String = "",
    val deliveryId: String = "",
    val deliveryPersonId: String = "",
    val deliveryPersonName: String = "",
    val rewardPoints: Int = 0,  // Points earned for this delivery
    val createdAt: Long = 0L,
    val deliveredAt: Long = 0L  // Timestamp when delivered
)

/**
 * Represents a single item within an order
 */
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val quantity: Int = 0,
    val subtotal: Double = 0.0
)

/**
 * Represents delivery person's reward stats
 */
data class DeliveryStats(
    val userId: String = "",
    val totalPoints: Int = 0,
    val totalDeliveries: Int = 0,
    val availablePoints: Int = 0,  // Points that can be redeemed
    val redeemedPoints: Int = 0,   // Points already converted to money
    val totalEarnings: Double = 0.0,
    val lastUpdated: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "totalPoints" to totalPoints,
            "totalDeliveries" to totalDeliveries,
            "availablePoints" to availablePoints,
            "redeemedPoints" to redeemedPoints,
            "totalEarnings" to totalEarnings,
            "lastUpdated" to lastUpdated
        )
    }
}