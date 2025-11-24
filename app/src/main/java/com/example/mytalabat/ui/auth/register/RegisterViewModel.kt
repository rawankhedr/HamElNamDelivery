package com.example.mytalabat.ui.auth.register

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytalabat.data.model.User
import com.example.mytalabat.data.model.UserProfile
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.UserRepository
import com.example.mytalabat.util.Resource
import com.example.mytalabat.util.ValidationUtil
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _registerState = MutableLiveData<Resource<User>>()
    val registerState: LiveData<Resource<User>> = _registerState

    fun register(name: String, email: String, phoneNumber: String, password: String) {
        Log.d("RegisterViewModel", "Starting registration for: $email")

        if (!ValidationUtil.isValidName(name)) {
            _registerState.value = Resource.Error("Name must be at least 2 characters")
            return
        }

        if (!ValidationUtil.isValidEmail(email)) {
            _registerState.value = Resource.Error("Invalid email address")
            return
        }

        if (!ValidationUtil.isValidPhoneNumber(phoneNumber)) {
            _registerState.value = Resource.Error("Invalid phone number")
            return
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _registerState.value = Resource.Error("Password must be at least 6 characters")
            return
        }

        _registerState.value = Resource.Loading()

        viewModelScope.launch {
            try {
                Log.d("RegisterViewModel", "Calling auth signup")
                val authResult = authRepository.signUp(email, password)

                if (authResult is Resource.Success) {
                    Log.d("RegisterViewModel", "Auth success, saving profile")
                    val user = authResult.data!!
                    val profile = UserProfile(
                        uid = user.uid,
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber
                    )

                    val profileResult = userRepository.saveUserProfile(profile)

                    if (profileResult is Resource.Success) {
                        Log.d("RegisterViewModel", "Profile saved successfully")
                        _registerState.value = Resource.Success(user)
                    } else {
                        Log.e("RegisterViewModel", "Profile save failed: ${profileResult.message}")
                        _registerState.value = Resource.Error("Failed to save profile: ${profileResult.message}")
                    }
                } else {
                    Log.e("RegisterViewModel", "Auth failed: ${authResult.message}")
                    _registerState.value = authResult
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Exception during registration", e)
                _registerState.value = Resource.Error(e.message ?: "Registration failed")
            }
        }
    }
}