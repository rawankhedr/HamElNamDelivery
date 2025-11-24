package com.example.mytalabat.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.databinding.ActivityLoginBinding
import com.example.mytalabat.ui.auth.register.RegisterActivity
import com.example.mytalabat.ui.main.MainActivity
import com.example.mytalabat.util.Resource
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(AuthRepository(FirebaseDataSource()))
    }

    private var isLoggingIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        // ✅ Observe login result
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)

                is Resource.Success -> {
                    val uid = viewModel.getCurrentUserId()
                    if (uid != null) {
                        isLoggingIn = true
                        // 🔍 Fetch full profile to check if seller
                        viewModel.checkSellerStatus(uid)
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "Login failed. Try again.", Toast.LENGTH_LONG).show()
                    }
                }

                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // ✅ Observe seller status check
        viewModel.isSeller.observe(this) { result ->
            if (!isLoggingIn) return@observe

            when (result) {
                is Resource.Loading -> Unit

                is Resource.Success -> {
                    showLoading(false)
                    isLoggingIn = false

                    val isSeller = result.data == true
                    if (isSeller) {
                        // 🚫 Block sellers from logging in
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(
                            this,
                            "Access denied. Seller accounts cannot log into this app.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // ✅ Allow buyers
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                }

                is Resource.Error -> {
                    showLoading(false)
                    isLoggingIn = false
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(
                        this,
                        "Error checking profile: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isLoggingIn = true
            showLoading(true)
            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
