package com.example.mytalabat.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mytalabat.ui.home.HomeFragment
import com.example.mytalabat.ui.orders.OrdersFragment
import com.example.mytalabat.ui.profile.ProfileFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> OrdersFragment()
            2 -> ProfileFragment()
            else -> HomeFragment()
        }
    }
}