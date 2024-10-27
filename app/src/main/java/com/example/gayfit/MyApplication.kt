package com.example.gayfit

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// Якщо використовуєте Debug провайдер:
// import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this) // Ініціалізація Firebase
            Log.d("MyApplication", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to initialize Firebase", e)
        }

        try {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
               // PlayIntegrityAppCheckProviderFactory.getInstance()
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("MyApplication", "App Check initialized with SafetyNet")
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to initialize App Check", e)
        }
    }
}
