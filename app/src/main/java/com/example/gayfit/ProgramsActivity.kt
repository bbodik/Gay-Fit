package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gayfit.adapters.SharedWorkoutAdapter
import com.example.gayfit.databinding.ActivityProgramsBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.Serializable


class ProgramsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var workoutAdapter: SharedWorkoutAdapter
    private val workouts = mutableListOf<SharedWorkout>()
    var auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityProgramsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізуємо binding
        binding = ActivityProgramsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        binding.buttonCreateWorkout.setOnClickListener {
            val intent = Intent(this, CreateWorkoutActivity::class.java)
            startActivity(intent)
        }

        binding.recyclerViewWorkouts.layoutManager = LinearLayoutManager(this)
        workoutAdapter = SharedWorkoutAdapter(workouts) { selectedWorkout ->
            // Обробка вибору тренування
            selectWorkout(selectedWorkout)
        }
        binding.recyclerViewWorkouts.adapter = workoutAdapter

        fetchWorkouts()
    }

    override fun onResume() {
        super.onResume()
        fetchWorkouts()
    }

    private fun fetchWorkouts() {
        db.collection("shared_workouts")
            .orderBy("userCount", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                workouts.clear()
                for (document in result) {
                    val workout = document.toObject(SharedWorkout::class.java)
                    workout.id = document.id
                    workouts.add(workout)
                }
                workoutAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selectWorkout(workout: SharedWorkout) {
        val intent = Intent(this, WorkoutDetailsActivity::class.java).apply {
            putExtra("WORKOUT", workout)
        }
        startActivity(intent)
    }
    private fun createMuscleMassWorkout(): SharedWorkout {
        val exercises = listOf(
            Exercise("Жим лежачи", 4, 8),
            Exercise("Присідання зі штангою", 4, 8),
            Exercise("Станова тяга", 4, 8)
        )
        return SharedWorkout(
            id = "",
            creatorId = auth.currentUser?.uid ?: "",
            title = "Нарощування м’язової маси",
            exercises = exercises,
            userCount = 1
        )
    }

    private fun createFatBurningWorkout(): SharedWorkout {
        val exercises = listOf(
            Exercise("Біг на місці", 5, 1),
            Exercise("Берпі", 4, 15),
            Exercise("Скакалка", 5, 1)
        )
        return SharedWorkout(
            id = "",
            creatorId = auth.currentUser?.uid ?: "",
            title = "Спалювання жиру",
            exercises = exercises,
            userCount = 1
        )
    }

    private fun createEnduranceWorkout(): SharedWorkout {
        val exercises = listOf(
            Exercise("Біг на довгу дистанцію", 1, 30),
            Exercise("Планка", 3, 1),
            Exercise("Велосипед", 1, 60)
        )
        return SharedWorkout(
            id = "",
            creatorId = auth.currentUser?.uid ?: "",
            title = "Підвищення витривалості",
            exercises = exercises,
            userCount = 1
        )
    }
    private fun startWorkout(workout: SharedWorkout) {
        val intent = Intent(this, ExerciseActivity::class.java).apply {
            putExtra("EXERCISES", workout.exercises as Serializable)
            putExtra("WORKOUT_TITLE", workout.title)
        }
        startActivity(intent)
    }

}
