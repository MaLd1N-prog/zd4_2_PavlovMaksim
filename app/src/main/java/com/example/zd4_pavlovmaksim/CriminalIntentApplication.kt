package com.example.zd4_pavlovmaksim

import android.app.Application
import com.example.zd4_pavlovmaksim.database.CrimeRepository

class CriminalIntentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}