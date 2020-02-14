package com.shubhampandey.snaprecipes

import com.google.firebase.database.FirebaseDatabase

class MyFirebasePersistenceClass: android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        // enable disk persistence
        val database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)
    }
}