package com.example.mytalabat.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mytalabat.R
import com.example.mytalabat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNavigation()
    }

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Disable over-scroll effect for smoother experience
        binding.viewPager.isUserInputEnabled = true

        // Sync ViewPager with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> binding.bottomNavigation.selectedItemId = R.id.nav_home
                    1 -> binding.bottomNavigation.selectedItemId = R.id.nav_orders
                    2 -> binding.bottomNavigation.selectedItemId = R.id.nav_profile
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_orders -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_profile -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }
    }
}