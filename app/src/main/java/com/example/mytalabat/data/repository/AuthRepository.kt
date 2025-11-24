package com.example.mytalabat.data.repository

import com.example.mytalabat.data.model.User
import com.example.mytalabat.data.model.UserProfile
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val dataSource: FirebaseDataSource) {

    // 🔹 Sign in existing user
    suspend fun signIn(email: String, password: String): Resource<User> {
        return withContext(Dispatchers.IO) {
            try {
                val user = dataSource.signIn(email, password)
                Resource.Success(user)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Login failed.")
            }
        }
    }

    // 🔹 Register new user
    suspend fun signUp(email: String, password: String): Resource<User> {
        return withContext(Dispatchers.IO) {
            try {
                val user = dataSource.signUp(email, password)
                Resource.Success(user)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Registration failed.")
            }
        }
    }

    // 🔹 Fetch full user profile from Firebase (includes isSeller flag)
    suspend fun getUserProfile(uid: String): Resource<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = dataSource.getUserProfile(uid)
                if (profile != null) {
                    Resource.Success(profile)
                } else {
                    Resource.Error("User profile not found.")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to fetch user profile.")
            }
        }
    }

    // 🔹 Get current authenticated Firebase user
    fun getCurrentUser() = dataSource.getCurrentUser()

    // 🔹 Sign out user
    fun signOut() = dataSource.signOut()
}
