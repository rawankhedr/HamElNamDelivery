package com.example.mytalabat

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MyTalabatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // CRITICAL: Set the correct database URL for Europe region
        FirebaseDatabase.getInstance()
            .setPersistenceEnabled(true) // Enable offline persistence
        FirebaseDatabase.getInstance()
            .reference
            .keepSynced(true)
    }
}