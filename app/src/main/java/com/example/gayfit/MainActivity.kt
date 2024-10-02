package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        val response = res.idpResponse
        if (res.resultCode == RESULT_OK) {
            // Вхід успішний
            Toast.makeText(this, "Вхід успішний", Toast.LENGTH_SHORT).show()
            setupMainUI()
        } else {
            // Вхід не вдався
            Toast.makeText(this, "Вхід не вдався: ${response?.error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Ініціалізація Firebase
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            // Користувач не увійшов, показати екран входу
            showSignInOptions()
        } else {
            // Користувач увійшов, показати MainActivity
            setupMainUI()
        }
    }

    private fun showSignInOptions() {
        // Налаштування провайдерів
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
            // Можеш додати інші провайдери, якщо потрібно
        )

        // Створення і запуск Intent для входу
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false) // Вимкнути SmartLock для налагодження
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun setupMainUI() {
        // Ініціалізація UI елементів MainActivity
        val startWorkoutButton = findViewById<Button>(R.id.startWorkoutButton)
        val workoutHistoryButton = findViewById<Button>(R.id.workoutHistoryButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val programsButton = findViewById<Button>(R.id.programsButton)

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

        programsButton.setOnClickListener {
            val intent = Intent(this, ProgramsActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            AuthUI.getInstance().signOut(this).addOnCompleteListener {
                Toast.makeText(this, "Ви вийшли з облікового запису", Toast.LENGTH_SHORT).show()
                showSignInOptions()
            }
        }
    }
}
