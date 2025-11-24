package com.example.mytalabat.data.model

data class Product(
    val productId: String = "",
    val productName: String = "",
    val description: String = "",
    val picURL: String = "",
    val productPrice: Double = 0.0,
    val quantityLeft: Int = 0,
    val shopId: String = "",
    val shopName: String = "",
    val category: String = "",
    val isAvailable: Boolean = true,
    val createdAt: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "productId" to productId,
            "productName" to productName,
            "description" to description,
            "picURL" to picURL,
            "productPrice" to productPrice,
            "quantityLeft" to quantityLeft,
            "shopId" to shopId,
            "shopName" to shopName,
            "category" to category,
            "isAvailable" to isAvailable,
            "createdAt" to createdAt
        )
    }
}