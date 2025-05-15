package com.example.gayfit

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

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
            // Використовуйте DebugAppCheckProviderFactory для розробки
            // Замініть на PlayIntegrityAppCheckProviderFactory для продакшн
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(

                    DebugAppCheckProviderFactory.getInstance()

                    //PlayIntegrityAppCheckProviderFactory.getInstance()

            )
            Log.d("MyApplication", "App Check initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to initialize App Check", e)
        }
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

    }
}
