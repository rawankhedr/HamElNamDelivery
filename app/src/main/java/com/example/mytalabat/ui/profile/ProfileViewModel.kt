package com.example.mytalabat.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mytalabat.data.model.UserProfile
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.UserRepository
import com.example.mytalabat.util.Resource
import com.example.mytalabat.util.S3Manager
import com.example.mytalabat.util.ValidationUtil
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _userProfile = MutableLiveData<Resource<UserProfile>>()
    val userProfile: LiveData<Resource<UserProfile>> = _userProfile

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    private val _photoUploadState = MutableLiveData<Resource<String>>()
    val photoUploadState: LiveData<Resource<String>> = _photoUploadState

    init {
        // Initialize S3Manager
        S3Manager.initialize(application.applicationContext)
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid
        Log.d("ProfileViewModel", "Loading profile for uid: $uid")

        if (uid == null) {
            Log.e("ProfileViewModel", "No authenticated user")
            _userProfile.value = Resource.Error("User not authenticated")
            return
        }

        _userProfile.value = Resource.Loading()
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Fetching user profile from Firebase")
                val result = userRepository.getUserProfile(uid)
                Log.d("ProfileViewModel", "Profile fetch result: ${result::class.simpleName}")
                _userProfile.value = result
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
                _userProfile.value = Resource.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun reloadProfile() {
        loadUserProfile()
    }

    fun updateProfile(name: String, phoneNumber: String) {
        if (!ValidationUtil.isValidName(name)) {
            _updateState.value = Resource.Error("Name must be at least 2 characters")
            return
        }

        if (!ValidationUtil.isValidPhoneNumber(phoneNumber)) {
            _updateState.value = Resource.Error("Invalid phone number")
            return
        }

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _updateState.value = Resource.Error("User not authenticated")
            return
        }

        _updateState.value = Resource.Loading()

        viewModelScope.launch {
            val updates = mapOf(
                "name" to name,
                "phoneNumber" to phoneNumber
            )
            val result = userRepository.updateUserProfile(uid, updates)
            _updateState.value = result

            if (result is Resource.Success) {
                loadUserProfile()
            }
        }
    }

    fun uploadProfilePhoto(photoUri: Uri) {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _photoUploadState.value = Resource.Error("User not authenticated")
            return
        }

        _photoUploadState.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Upload to S3
                val s3Url = S3Manager.uploadProfilePhoto(
                    getApplication(),
                    uid,
                    photoUri
                )

                Log.d("ProfileViewModel", "Photo uploaded to S3: $s3Url")

                // Update Firebase with the S3 URL
                val updates = mapOf("profilePhotoUrl" to s3Url)
                val updateResult = userRepository.updateUserProfile(uid, updates)

                if (updateResult is Resource.Success) {
                    _photoUploadState.value = Resource.Success(s3Url)
                    // Reload profile to get updated data
                    loadUserProfile()
                } else {
                    _photoUploadState.value = Resource.Error("Failed to update profile with photo URL")
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error uploading photo", e)
                _photoUploadState.value = Resource.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}