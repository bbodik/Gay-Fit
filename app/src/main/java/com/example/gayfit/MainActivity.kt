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

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

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
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Користувач увійшов, показати MainActivity
            setupMainUI()
        }
    }

    private fun setupMainUI() {
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
                auth.signOut()
                Toast.makeText(this, "Ви вийшли з облікового запису", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
