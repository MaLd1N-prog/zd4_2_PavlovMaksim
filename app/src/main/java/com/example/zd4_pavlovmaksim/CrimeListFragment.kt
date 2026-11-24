package com.example.zd4_pavlovmaksim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CrimeListFragment : Fragment() {

    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var emptyView: View
    private val adapter = CrimeAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view)
        emptyView = view.findViewById(R.id.empty_view)
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        view.findViewById<Button>(R.id.empty_add_button).setOnClickListener { addNewCrime() }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CrimeLab.get(requireActivity()).getCrimes()
            .observe(viewLifecycleOwner) { crimes -> updateUI(crimes ?: emptyList()) }
    }

    private fun updateUI(crimes: List<Crime>) {
        adapter.crimes = crimes
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (crimes.isEmpty()) View.VISIBLE else View.GONE
        crimeRecyclerView.visibility = if (crimes.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.new_crime -> { addNewCrime(); true }
            else -> super.onOptionsItemSelected(item)
        }

    private fun addNewCrime() {
        val crime = Crime()
        CrimeLab.get(requireActivity()).addCrime(crime)
        startActivity(CrimePagerActivity.newIntent(requireContext(), crime.id))
    }

    private inner class CrimeHolder(view: View) :
        RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var crime: Crime
        private val titleTextView: TextView = view.findViewById(R.id.crime_title)
        private val dateTextView: TextView = view.findViewById(R.id.crime_date)
        private val suspectTextView: TextView = view.findViewById(R.id.crime_suspect_name)
        private val solvedTextView: TextView = view.findViewById(R.id.crime_solved_mark)

        init { itemView.setOnClickListener(this) }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = crime.title.ifBlank { getString(R.string.untitled_crime) }
            dateTextView.text =
                android.text.format.DateFormat.format("EEE, MMM dd yyyy", crime.date)
            // Выбранный подозреваемый виден прямо в списке (LiveData обновит его сама)
            suspectTextView.text =
                if (crime.suspect.isBlank()) getString(R.string.crime_report_no_suspect)
                else getString(R.string.crime_report_suspect, crime.suspect)
            solvedTextView.visibility = if (crime.isSolved) View.VISIBLE else View.GONE
        }

        override fun onClick(v: View) {
            startActivity(CrimePagerActivity.newIntent(requireContext(), crime.id))
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>) :
        RecyclerView.Adapter<CrimeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun getItemCount(): Int = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            holder.bind(crimes[position])
        }
    }
}