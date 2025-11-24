package com.example.mytalabat.data.model

data class Shop(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePhotoUrl: String = "",
    val createdAt: Long = 0L
) {
    companion object {
        fun fromUserProfile(profile: UserProfile): Shop {
            return Shop(
                uid = profile.uid,
                name = profile.name,
                email = profile.email,
                phoneNumber = profile.phoneNumber,
                profilePhotoUrl = profile.profilePhotoUrl,
                createdAt = profile.createdAt
            )
        }
    }
}