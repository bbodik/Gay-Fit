// StartWorkoutActivity.kt
package com.example.gayfit

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityStartWorkoutBinding
import com.example.gayfit.models.ExerciseInWorkout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StartWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartWorkoutBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var workoutExercises: List<ExerciseInWorkout> = listOf()
    private var currentExerciseIndex = 0
    private var completedExercises: MutableList<ExerciseInWorkout> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Отримання переданого списку вправ
        workoutExercises = intent.getSerializableExtra("WORKOUT_EXERCISES") as? List<ExerciseInWorkout> ?: listOf()

        if (workoutExercises.isEmpty()) {
            Toast.makeText(this, "Виберіть вправи для тренування", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ініціалізація першої вправи
        displayCurrentExercise()

        binding.buttonNextExercise.setOnClickListener {
            saveCurrentExerciseData()
        }
    }

    private fun displayCurrentExercise() {
        if (currentExerciseIndex >= workoutExercises.size) {
            // Завершення тренування
            finishWorkout()
            return
        }

        val exercise = workoutExercises[currentExerciseIndex]
        binding.textViewCurrentExercise.text = exercise.exercise.name
        binding.textViewExerciseDescription.text = exercise.exercise.description

        // Оновлення прогрес бару
        val progress = ((currentExerciseIndex) / workoutExercises.size.toFloat()) * 100
        binding.progressBarWorkout.progress = progress.toInt()

        // Очистка полів вводу
        binding.editTextSets.setText("")
        binding.editTextReps.setText("")
        binding.editTextWeight.setText("")
    }

    private fun saveCurrentExerciseData() {
        val sets = binding.editTextSets.text.toString().toIntOrNull()
        val reps = binding.editTextReps.text.toString().toIntOrNull()
        val weight = binding.editTextWeight.text.toString().toFloatOrNull()

        if (sets == null || reps == null || weight == null || sets <= 0 || reps <= 0 || weight < 0) {
            Toast.makeText(this, "Будь ласка, введіть коректні значення", Toast.LENGTH_SHORT).show()
            return
        }

        // Збереження даних для поточної вправи
        val exerciseInWorkout = workoutExercises[currentExerciseIndex]
        exerciseInWorkout.sets = sets
        exerciseInWorkout.reps = reps
        exerciseInWorkout.weight = weight

        completedExercises.add(exerciseInWorkout)

        currentExerciseIndex++

        // Відображення наступної вправи
        displayCurrentExercise()
    }

    private fun finishWorkout() {
        // Збереження завершеного тренування в Firestore
        val userId = auth.currentUser?.uid ?: ""
        val workoutData = hashMapOf(
            "userId" to userId,
            "exercises" to completedExercises,
            "completedAt" to System.currentTimeMillis()
        )

        db.collection("workout_results")
            .add(workoutData)
            .addOnSuccessListener {
                Toast.makeText(this, "Тренування завершено!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка при збереженні тренування: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
