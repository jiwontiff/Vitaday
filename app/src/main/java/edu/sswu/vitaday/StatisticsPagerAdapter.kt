package edu.sswu.vitaday

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class StatisticsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AccumulatedTimeFragment()
            1 -> SubjectDistributionFragment()
            2 -> StudyGraphFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}