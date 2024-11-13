// ProgramsActivity.kt
package com.example.gayfit

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gayfit.adapters.SharedWorkoutAdapter
import com.example.gayfit.databinding.ActivityProgramsBinding
import com.example.gayfit.models.SharedWorkout
import com.example.gayfit.models.WorkoutItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgramsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var workoutAdapter: SharedWorkoutAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityProgramsBinding
    private lateinit var tabLayout: TabLayout
    private val items = mutableListOf<WorkoutItem>()
    private var currentTab = 0 // 0 - онлайн, 1 - збережені
    private var allWorkouts = listOf<WorkoutItem>() // Для фільтрації

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізуємо binding
        binding = ActivityProgramsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Налаштовуємо Toolbar
        setSupportActionBar(binding.toolbar)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Налаштовуємо FloatingActionButton
        binding.fabCreateWorkout.setOnClickListener {
            val intent = Intent(this, CreateWorkoutActivity::class.java)
            startActivity(intent)
        }

        // Налаштовуємо RecyclerView
        binding.recyclerViewWorkouts.layoutManager = LinearLayoutManager(this)
        workoutAdapter = SharedWorkoutAdapter(
            items = items,
            onWorkoutSelected = { selectedWorkout ->
                selectWorkout(selectedWorkout)
            },
            onSavedWorkoutSelected = { savedWorkout ->
                selectSavedWorkout(savedWorkout)
            }
        )
        binding.recyclerViewWorkouts.adapter = workoutAdapter

        // Ініціалізуємо TabLayout
        tabLayout = binding.tabLayout

        // Додаємо вкладки програмно
        tabLayout.addTab(tabLayout.newTab().setText("Онлайн тренування"))
        tabLayout.addTab(tabLayout.newTab().setText("Мої тренування"))

        // Налаштовуємо слухач подій для вкладок
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                if (currentTab == 0) {
                    fetchWorkouts()
                } else {
                    loadSavedWorkouts()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Завантажуємо дані відповідно до наявності інтернету
        if (isNetworkAvailable()) {
            fetchWorkouts()
        } else {
            loadSavedWorkouts()
            Toast.makeText(this, "Немає інтернет-з'єднання. Завантажено збережені тренування.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) {
            fetchWorkouts()
        } else {
            loadSavedWorkouts()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_programs, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.queryHint = "Пошук тренувань..."

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Не використовується
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterWorkouts(newText)
                return true
            }
        })

        return true
    }

    private fun filterWorkouts(query: String?) {
        if (query.isNullOrEmpty()) {
            workoutAdapter.updateItems(allWorkouts)
            return
        }

        val filteredList = allWorkouts.filter { item ->
            when (item) {
                is WorkoutItem.Online -> item.workout.title.contains(query, ignoreCase = true) ||
                        item.workout.description.contains(query, ignoreCase = true)
                is WorkoutItem.Saved -> item.workout.title.contains(query, ignoreCase = true) ||
                        item.workout.description.contains(query, ignoreCase = true)
            }
        }

        workoutAdapter.updateItems(filteredList)
    }

    private fun loadSavedWorkouts() {
        CoroutineScope(Dispatchers.IO).launch {
            val savedWorkouts = WorkoutDatabase.getDatabase(this@ProgramsActivity)
                .workoutDao()
                .getSavedWorkouts()
                .map { WorkoutItem.Saved(it) }

            Log.d("ProgramsActivity", "Завантажено збережені тренування: $savedWorkouts")

            items.clear()
            items.addAll(savedWorkouts)
            allWorkouts = items.toList() // Зберігаємо копію для фільтрації
            runOnUiThread {
                workoutAdapter.updateItems(items)
            }
        }
    }


    private fun fetchWorkouts() {
        db.collection("shared_workouts")
            .orderBy("userCount", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val onlineWorkouts = result.map { document ->
                    val workout = document.toObject(SharedWorkout::class.java)
                    workout.id = document.id
                    WorkoutItem.Online(workout)
                }
                items.clear()
                items.addAll(onlineWorkouts)
                allWorkouts = items.toList() // Зберігаємо копію для фільтрації
                workoutAdapter.updateItems(items)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selectWorkout(workout: SharedWorkout) {
        val intent = Intent(this, WorkoutDetailsActivity::class.java).apply {
            putExtra("WORKOUT_ID", workout.id)
        }
        startActivity(intent)
    }

    private fun selectSavedWorkout(workout: SavedWorkoutEntity) {
        val intent = Intent(this, ExerciseActivity::class.java).apply {
            putExtra("EXERCISES", workout.exercises as ArrayList)
            putExtra("WORKOUT_TITLE", workout.title)
        }
        startActivity(intent)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
