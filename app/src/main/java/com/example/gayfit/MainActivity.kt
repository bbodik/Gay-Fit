// MainActivity.kt
package com.example.gayfit

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.FirebaseAppCheck


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if the user is already signed in
        if(auth.currentUser == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val startWorkoutButton = findViewById<Button>(R.id.startWorkoutButton)
        val workoutHistoryButton = findViewById<Button>(R.id.workoutHistoryButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton) // Add logout button
        val programsButton = findViewById<Button>(R.id.programsButton)

        programsButton.setOnClickListener {
            val intent = Intent(this, ProgramsActivity::class.java)
            startActivity(intent)
        }

        startWorkoutButton.setOnClickListener {
            val intent = Intent(this, StartWorkoutActivity::class.java)
            startActivity(intent)
        }

        workoutHistoryButton.setOnClickListener {
            val intent = Intent(this, WorkoutHistoryActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}


