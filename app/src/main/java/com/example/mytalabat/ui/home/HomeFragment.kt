package com.example.mytalabat.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mytalabat.data.remote.FirebaseDataSource
import com.example.mytalabat.data.repository.AuthRepository
import com.example.mytalabat.data.repository.OrderRepository
import com.example.mytalabat.databinding.FragmentHomeBinding
import com.example.mytalabat.util.Constants
import com.example.mytalabat.util.Resource

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val dataSource = FirebaseDataSource()
        HomeViewModelFactory(
            AuthRepository(dataSource),
            OrderRepository(dataSource)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.deliveryStats.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    resource.data?.let { stats ->
                        displayStats(stats)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showEmptyState()
                    Toast.makeText(
                        requireContext(),
                        "Error loading stats: ${resource.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displayStats(stats: com.example.mytalabat.data.model.DeliveryStats) {
        // Show all cards
        binding.cardWelcome.visibility = View.VISIBLE
        binding.layoutStatsGrid.visibility = View.VISIBLE
        binding.tvPointsTitle.visibility = View.VISIBLE
        binding.cardAvailablePoints.visibility = View.VISIBLE
        binding.cardRedeemedPoints.visibility = View.VISIBLE
        binding.cardInfo.visibility = View.VISIBLE

        // Total Earnings (big orange card)
        binding.tvTotalEarnings.text = String.format("%.2f EGP", stats.totalEarnings)

        // Stats Grid
        binding.tvTotalDeliveries.text = stats.totalDeliveries.toString()
        binding.tvTotalPoints.text = stats.totalPoints.toString()

        // Available Points
        binding.tvAvailablePoints.text = stats.availablePoints.toString()
        val pointsValue = stats.availablePoints * Constants.EGP_PER_POINT
        binding.tvPointsValue.text = String.format("%.2f EGP", pointsValue)

        // Redeemed Points
        binding.tvRedeemedPoints.text = stats.redeemedPoints.toString()
    }

    private fun showEmptyState() {
        // Show empty state with 0 values
        binding.cardWelcome.visibility = View.VISIBLE
        binding.layoutStatsGrid.visibility = View.VISIBLE
        binding.tvPointsTitle.visibility = View.VISIBLE
        binding.cardAvailablePoints.visibility = View.VISIBLE
        binding.cardRedeemedPoints.visibility = View.VISIBLE
        binding.cardInfo.visibility = View.VISIBLE

        binding.tvTotalEarnings.text = "0.00 EGP"
        binding.tvTotalDeliveries.text = "0"
        binding.tvTotalPoints.text = "0"
        binding.tvAvailablePoints.text = "0"
        binding.tvPointsValue.text = "0.00 EGP"
        binding.tvRedeemedPoints.text = "0"
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.cardWelcome.visibility = View.GONE
            binding.layoutStatsGrid.visibility = View.GONE
            binding.tvPointsTitle.visibility = View.GONE
            binding.cardAvailablePoints.visibility = View.GONE
            binding.cardRedeemedPoints.visibility = View.GONE
            binding.cardInfo.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
