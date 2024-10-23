// StartWorkoutActivity.kt
package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityStartWorkoutBinding
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class StartWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartWorkoutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var workout: SharedWorkout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutSerializable = intent.getSerializableExtra("WORKOUT") as? SharedWorkout

        if (workoutId != null) {
            // Завантаження тренування з Firestore
            db.collection("shared_workouts").document(workoutId)
                .get()
                .addOnSuccessListener { document ->
                    val workout = document.toObject(SharedWorkout::class.java)
                    if (workout != null) {
                        this.workout = workout
                        startExerciseActivity()
                    } else {
                        Toast.makeText(this, "Тренування не знайдено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } else if (workoutSerializable != null) {
            // Якщо тренування передано напряму
            this.workout = workoutSerializable
            startExerciseActivity()
        } else {
            Toast.makeText(this, "Немає даних про тренування", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startExerciseActivity() {
        val intent = Intent(this, ExerciseActivity::class.java).apply {
            putExtra("EXERCISES", workout.exercises as Serializable)
            putExtra("WORKOUT_TITLE", workout.title)
        }
        startActivity(intent)
        finish()
    }
}
