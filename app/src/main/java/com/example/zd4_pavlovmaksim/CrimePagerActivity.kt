package com.example.zd4_pavlovmaksim

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.util.UUID

private const val EXTRA_CRIME_ID = "com.example.zd4_pavlovmaksim.crime_id"

class CrimePagerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var crimes: List<Crime> = emptyList()
    private var initialPositionSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crime_pager)

        viewPager = findViewById(R.id.crime_view_pager)
        viewPager.adapter = CrimePagerAdapter()

        val crimeId = intent.getSerializableExtra(EXTRA_CRIME_ID) as UUID
        CrimeLab.get(this).getCrimes().observe(this) { list ->
            crimes = list ?: emptyList()
            viewPager.adapter?.notifyDataSetChanged()
            if (!initialPositionSet) {
                val index = crimes.indexOfFirst { it.id == crimeId }
                if (index >= 0) {
                    viewPager.currentItem = index
                    initialPositionSet = true
                }
            }
        }
    }

    private inner class CrimePagerAdapter : FragmentStateAdapter(this@CrimePagerActivity) {
        override fun getItemCount(): Int = crimes.size
        override fun createFragment(position: Int): Fragment =
            CrimeFragment.newInstance(crimes[position].id)
    }

    companion object {
        fun newIntent(context: Context, crimeId: UUID): Intent =
            Intent(context, CrimePagerActivity::class.java).apply {
                putExtra(EXTRA_CRIME_ID, crimeId)
            }
    }
}