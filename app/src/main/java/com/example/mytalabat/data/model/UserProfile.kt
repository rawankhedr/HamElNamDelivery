package com.example.mytalabat.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePhotoUrl: String = "", // ✅ For profile pictures
    val isSeller: Boolean = false,    // ✅ Critical field to distinguish sellers
    val createdAt: Long = System.currentTimeMillis()
) {
    // ✅ No-argument constructor required by Firebase
    constructor() : this("", "", "", "", "", false, 0L)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "profilePhotoUrl" to profilePhotoUrl,
            "isSeller" to isSeller, // ✅ Include in Firebase map
            "createdAt" to createdAt
        )
    }
}
