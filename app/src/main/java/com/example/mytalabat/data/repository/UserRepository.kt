package com.example.mytalabat.data.repository

import android.util.Log
import com.example.mytalabat.data.model.UserProfile
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val dataSource: FirebaseDataSource) {

    // 🔹 Save the full user profile (including isSeller)
    suspend fun saveUserProfile(profile: UserProfile): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Saving user profile: $profile")
                dataSource.saveUserProfile(profile)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("UserRepository", "Error saving profile: ${e.message}")
                Resource.Error(e.message ?: "Failed to save profile")
            }
        }
    }

    // 🔹 Retrieve user profile (includes isSeller flag)
    suspend fun getUserProfile(uid: String): Resource<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Fetching profile for UID: $uid")
                val profile = dataSource.getUserProfile(uid)
                if (profile != null) {
                    Log.d("UserRepository", "Profile fetched: $profile (isSeller=${profile.isSeller})")
                    Resource.Success(profile)
                } else {
                    Log.e("UserRepository", "Profile not found for UID: $uid")
                    Resource.Error("Profile not found")
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error fetching profile: ${e.message}")
                Resource.Error(e.message ?: "Failed to fetch profile")
            }
        }
    }

    // 🔹 Update full profile (can include isSeller, name, phone, etc.)
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Updating profile for UID: $uid with $updates")
                dataSource.updateUserProfile(uid, updates)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("UserRepository", "Error updating profile: ${e.message}")
                Resource.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    // 🔹 Update only the profile picture URL
    suspend fun updateProfilePictureUrl(uid: String, url: String): Resource<Unit> {
        return updateUserProfile(uid, mapOf("profilePictureUrl" to url))
    }
}
