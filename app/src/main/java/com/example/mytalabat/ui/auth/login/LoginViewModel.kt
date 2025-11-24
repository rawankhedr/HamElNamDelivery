package com.example.mytalabat.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytalabat.data.model.User
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.util.Resource
import com.example.mytalabat.util.ValidationUtil
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<User>>()
    val loginState: LiveData<Resource<User>> get() = _loginState

    private val _isSeller = MutableLiveData<Resource<Boolean>>()
    val isSeller: LiveData<Resource<Boolean>> get() = _isSeller

    fun login(email: String, password: String) {
        if (!ValidationUtil.isValidEmail(email)) {
            _loginState.value = Resource.Error("Invalid email address.")
            return
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _loginState.value = Resource.Error("Password must be at least 6 characters.")
            return
        }

        _loginState.value = Resource.Loading()

        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _loginState.postValue(result)
        }
    }

    fun getCurrentUserId(): String? = authRepository.getCurrentUser()?.uid

    /**
     * Fetch user profile from Firebase and determine if user is a seller.
     */
    fun checkSellerStatus(userId: String) {
        if (userId.isBlank()) {
            _isSeller.value = Resource.Error("Invalid user ID.")
            return
        }

        _isSeller.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = authRepository.getUserProfile(userId)
                when (result) {
                    is Resource.Success -> {
                        val profile = result.data
                        if (profile != null) {
                            _isSeller.postValue(Resource.Success(profile.isSeller))
                        } else {
                            _isSeller.postValue(Resource.Error("User profile not found."))
                        }
                    }

                    is Resource.Error -> {
                        _isSeller.postValue(Resource.Error(result.message ?: "Failed to load user profile."))
                    }

                    else -> Unit
                }
            } catch (e: Exception) {
                _isSeller.postValue(Resource.Error("Error fetching seller status: ${e.message}"))
            }
        }
    }

    fun isUserLoggedIn(): Boolean = authRepository.getCurrentUser() != null
}
