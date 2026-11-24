package com.example.zd4_pavlovmaksim.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.zd4_pavlovmaksim.Crime
import java.util.UUID

@Dao
interface CrimeDao {

    @Query("SELECT * FROM Crime")
    fun getCrimes(): LiveData<List<Crime>>

    @Query("SELECT * FROM Crime WHERE id = :id")
    fun getCrime(id: UUID): LiveData<Crime?>

    @Insert
    fun addCrime(crime: Crime)

    @Update
    fun updateCrime(crime: Crime)

    @Delete
    fun deleteCrime(crime: Crime)
}