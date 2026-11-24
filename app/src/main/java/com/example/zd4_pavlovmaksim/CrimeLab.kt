package com.example.zd4_pavlovmaksim

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.zd4_pavlovmaksim.database.CrimeRepository
import java.util.UUID

class CrimeLab private constructor(context: Context) {

    private val crimeRepository = CrimeRepository.get()

    fun getCrimes(): LiveData<List<Crime>> = crimeRepository.getCrimes()
    fun getCrime(id: UUID): LiveData<Crime?> = crimeRepository.getCrime(id)
    fun addCrime(crime: Crime) = crimeRepository.addCrime(crime)
    fun updateCrime(crime: Crime) = crimeRepository.updateCrime(crime)
    fun deleteCrime(crime: Crime) = crimeRepository.deleteCrime(crime)

    companion object {
        private var INSTANCE: CrimeLab? = null

        fun get(context: Context): CrimeLab {
            return INSTANCE ?: CrimeLab(context.applicationContext).also { INSTANCE = it }
        }
    }
}