package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gayfit.adapters.ExerciseAdapter
import com.example.gayfit.databinding.ActivityWorkoutDetailsBinding
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailsBinding
    private lateinit var workout: SharedWorkout
    private lateinit var db: FirebaseFirestore
    private lateinit var buttonLikeWorkout: ImageButton
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        buttonLikeWorkout = binding.buttonLikeWorkout

        val workoutId = intent.getStringExtra("WORKOUT_ID")
        if (workoutId != null) {
            fetchWorkoutDetails(workoutId)
        } else {
            Toast.makeText(this, "ID тренування не передано", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Обробка кнопки початку тренування
        binding.buttonStartWorkout.setOnClickListener {
            startWorkout()
        }
    }

    private fun fetchWorkoutDetails(workoutId: String) {
        db.collection("shared_workouts").document(workoutId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    workout = document.toObject(SharedWorkout::class.java)!!
                    workout.id = document.id
                    setupUI()
                } else {
                    Toast.makeText(this, "Тренування не знайдено", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка при завантаженні тренування: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        // Відображаємо назву тренування
        binding.textViewWorkoutTitle.text = workout.title

        // Налаштовуємо RecyclerView для відображення вправ
        binding.recyclerViewExercises.layoutManager = LinearLayoutManager(this)
        val adapter = ExerciseAdapter(workout.exercises)
        binding.recyclerViewExercises.adapter = adapter

        // Перевірка, чи тренування збережено
        CoroutineScope(Dispatchers.IO).launch {
            val savedWorkout = WorkoutDatabase.getDatabase(this@WorkoutDetailsActivity)
                .workoutDao()
                .getSavedWorkoutById(workout.id)

            isLiked = savedWorkout != null
            runOnUiThread {
                updateLikeButton()
            }
        }

        // Обробка кнопки збереження
        buttonLikeWorkout.setOnClickListener {
            if (isLiked) {
                removeWorkoutFromFavorites()
            } else {
                saveWorkoutToFavorites()
            }
        }
    }

    private fun updateLikeButton() {
        if (isLiked) {
            buttonLikeWorkout.setImageResource(R.drawable.ic_heart_filled)
        } else {
            buttonLikeWorkout.setImageResource(R.drawable.ic_heart_outline)
        }
    }

    private fun saveWorkoutToFavorites() {
        CoroutineScope(Dispatchers.IO).launch {
            val savedWorkoutEntity = SavedWorkoutEntity(
                id = workout.id,
                title = workout.title,
                exercises = workout.exercises
            )
            WorkoutDatabase.getDatabase(this@WorkoutDetailsActivity)
                .workoutDao()
                .insertSavedWorkout(savedWorkoutEntity)
            isLiked = true
            runOnUiThread {
                updateLikeButton()
                Toast.makeText(this@WorkoutDetailsActivity, "Тренування збережено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeWorkoutFromFavorites() {
        CoroutineScope(Dispatchers.IO).launch {
            val savedWorkoutEntity = SavedWorkoutEntity(
                id = workout.id,
                title = workout.title,
                exercises = workout.exercises
            )
            WorkoutDatabase.getDatabase(this@WorkoutDetailsActivity)
                .workoutDao()
                .deleteSavedWorkout(savedWorkoutEntity)
            isLiked = false
            runOnUiThread {
                updateLikeButton()
                Toast.makeText(this@WorkoutDetailsActivity, "Тренування видалено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startWorkout() {
        // Збільшуємо лічильник користувачів
        val workoutRef = db.collection("shared_workouts").document(workout.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(workoutRef)
            val newCount = snapshot.getLong("userCount")?.plus(1) ?: 1
            transaction.update(workoutRef, "userCount", newCount)
        }.addOnSuccessListener {
            // Переходимо до ExerciseActivity
            val intent = Intent(this, ExerciseActivity::class.java).apply {
                putExtra("EXERCISES", ArrayList(workout.exercises))
                putExtra("WORKOUT_TITLE", workout.title)
            }
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
