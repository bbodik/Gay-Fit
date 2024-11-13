// MainActivity.kt
package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.gayfit.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

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
        // Ініціалізація View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Налаштування Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            // Користувач не увійшов, показати екран входу
            showSignInOptions()
        } else {
            // Користувач увійшов, показати MainActivity
            setupMainUI()
        }

        // Приклад кнопки виходу (якщо є у макеті)

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

    fun setupMainUI() {
        // Ініціалізація UI елементів MainActivity
        binding.apply {
            startWorkoutButton.setOnClickListener {
                val intent = Intent(this@MainActivity, ProgramsActivity::class.java)
                startActivity(intent)
            }

            workoutHistoryButton.setOnClickListener {
                val intent = Intent(this@MainActivity, WorkoutHistoryActivity::class.java)
                startActivity(intent)
            }

            statisticsButton.setOnClickListener {
                val intent = Intent(this@MainActivity, StatisticsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu) // Завантажуємо меню з res/menu/main_menu.xml
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                AuthUI.getInstance().signOut(this).addOnCompleteListener {
                    Toast.makeText(this, "Ви вийшли з облікового запису", Toast.LENGTH_SHORT).show()
                    showSignInOptions()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
