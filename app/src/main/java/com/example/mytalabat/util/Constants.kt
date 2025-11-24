package com.example.mytalabat.util

object Constants {
    const val USERS_REF = "users"
    const val PROFILES_REF = "profiles"
    const val PRODUCTS_REF = "products"
    const val ORDERS_REF = "orders"
    const val DELIVERY_STATS_REF = "deliveryStats"  // NEW
    const val PREFS_NAME = "mytalabat_prefs"
    const val KEY_USER_ID = "user_id"

    // NEW: Reward system configuration
    const val POINTS_PER_DELIVERY = 10      // Base points per delivery
    const val BONUS_POINTS_THRESHOLD = 100.0 // Bonus for orders over 100 EGP
    const val BONUS_POINTS = 5               // Extra points for large orders
    const val EGP_PER_POINT = 0.5            // 1 point = 0.5 EGP when redeemed
}