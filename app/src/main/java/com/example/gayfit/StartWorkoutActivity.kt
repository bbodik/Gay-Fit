package com.example.gayfit

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityStartWorkoutBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.SharedWorkout
import com.example.gayfit.models.Workout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StartWorkoutActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var workout: SharedWorkout
    private val completedExercises = mutableListOf<Exercise>()
    private lateinit var binding: ActivityStartWorkoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val saveWorkoutButton = binding.buttonSaveWorkout

        if (workoutId != null) {
            // Завантаження тренування з Firestore
            db.collection("shared_workouts").document(workoutId)
                .get()
                .addOnSuccessListener { document ->
                    workout = document.toObject(SharedWorkout::class.java)!!
                    displayExercises(workout.exercises)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Немає даних про тренування", Toast.LENGTH_SHORT).show()
            finish()
        }

        saveWorkoutButton.setOnClickListener {
            if (completedExercises.isNotEmpty()) {
                val userWorkout = Workout(
                    userId = auth.currentUser?.uid ?: "",
                    date = System.currentTimeMillis(),
                    exercises = completedExercises,
                    program = workout.title
                )
                db.collection("workouts")
                    .add(userWorkout)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Тренування збережено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Ви не виконали жодної вправи", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayExercises(exercises: List<Exercise>) {
        val exercisesLayout = binding.exercisesLayout
        exercises.forEach { exercise ->
            val checkBox = CheckBox(this).apply {
                text = "${exercise.name} - ${exercise.sets} сетів по ${exercise.reps} повторень"
                id = View.generateViewId()
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        completedExercises.add(exercise)
                    } else {
                        completedExercises.remove(exercise)
                    }
                }
            }
            exercisesLayout.addView(checkBox)
        }
    }
}
