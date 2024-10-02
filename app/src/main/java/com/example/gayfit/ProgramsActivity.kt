package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gayfit.adapters.SharedWorkoutAdapter
import com.example.gayfit.databinding.ActivityProgramsBinding
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query



class ProgramsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var workoutAdapter: SharedWorkoutAdapter
    private val workouts = mutableListOf<SharedWorkout>()

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
        // Збільшуємо лічильник користувачів
        val workoutRef = db.collection("shared_workouts").document(workout.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(workoutRef)
            val newCount = snapshot.getLong("userCount")?.plus(1) ?: 1
            transaction.update(workoutRef, "userCount", newCount)
        }.addOnSuccessListener {
            // Переходимо до активності початку тренування
            val intent = Intent(this, StartWorkoutActivity::class.java).apply {
                putExtra("WORKOUT_ID", workout.id)
            }
            startActivity(intent)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
