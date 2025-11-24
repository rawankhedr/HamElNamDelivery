package com.example.mytalabat.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mytalabat.R
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.UserRepository
import com.example.mytalabat.databinding.FragmentProfileBinding
import com.example.mytalabat.ui.auth.login.LoginActivity
import com.example.mytalabat.util.Resource

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val dataSource = FirebaseDataSource()
        ProfileViewModelFactory(
            requireActivity().application, // Add this
            AuthRepository(dataSource),
            UserRepository(dataSource)
        )
    }

    private var isEditMode = false
    private var selectedPhotoUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri
                // Display selected image immediately
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.ivProfilePhoto)

                // Auto-upload the photo
                viewModel.uploadProfilePhoto(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            Log.d("ProfileFragment", "Profile state: ${resource::class.simpleName}")
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { profile ->
                        Log.d("ProfileFragment", "Displaying profile: ${profile.name}, ${profile.phoneNumber}")
                        binding.etName.setText(profile.name)
                        binding.etPhone.setText(profile.phoneNumber)
                        binding.tvEmail.text = profile.email

                        // Load profile photo from S3 URL
                        if (profile.profilePhotoUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(profile.profilePhotoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivProfilePhoto)
                        } else {
                            // Set default placeholder
                            binding.ivProfilePhoto.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                    Log.e("ProfileFragment", "Error loading profile: ${resource.message}")
                }
            }
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnEdit.isEnabled = false
                    binding.btnLogout.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnEdit.isEnabled = true
                    binding.btnLogout.isEnabled = true
                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                    toggleEditMode()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnEdit.isEnabled = true
                    binding.btnLogout.isEnabled = true
                    Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe photo upload state
        viewModel.photoUploadState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnChangePhoto.isEnabled = false
                    Toast.makeText(context, "Uploading photo...", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnChangePhoto.isEnabled = true
                    Toast.makeText(context, "Photo updated successfully!", Toast.LENGTH_SHORT).show()
                    // Photo is already displayed from local URI
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnChangePhoto.isEnabled = true
                    Toast.makeText(context, "Failed to upload photo: ${resource.message}", Toast.LENGTH_LONG).show()
                    // Reload the profile to show the old photo
                    viewModel.reloadProfile()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener {
            if (isEditMode) {
                val name = binding.etName.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()
                viewModel.updateProfile(name, phone)
            } else {
                toggleEditMode()
            }
        }

        binding.btnLogout.setOnClickListener {
            viewModel.signOut()
            navigateToLogin()
        }

        // Change photo button
        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Click on profile photo to change
        binding.ivProfilePhoto.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        binding.etName.isEnabled = isEditMode
        binding.etPhone.isEnabled = isEditMode
        binding.btnEdit.text = if (isEditMode) "Save" else "Edit Profile"
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}