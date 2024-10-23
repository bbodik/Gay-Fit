package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gayfit.adapters.ExerciseAdapter
import com.example.gayfit.databinding.ActivityWorkoutDetailsBinding
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class WorkoutDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailsBinding
    private lateinit var workout: SharedWorkout
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Отримуємо тренування з Intent
        workout = intent.getSerializableExtra("WORKOUT") as SharedWorkout

        // Відображаємо назву тренування
        binding.textViewWorkoutTitle.text = workout.title

        // Налаштовуємо RecyclerView для відображення вправ
        binding.recyclerViewExercises.layoutManager = LinearLayoutManager(this)
        val adapter = ExerciseAdapter(workout.exercises)
        binding.recyclerViewExercises.adapter = adapter

        // Обробка кнопки початку тренування
        binding.buttonStartWorkout.setOnClickListener {
            startWorkout()
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
                putExtra("EXERCISES", workout.exercises as Serializable)
                putExtra("WORKOUT_TITLE", workout.title)
            }
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
