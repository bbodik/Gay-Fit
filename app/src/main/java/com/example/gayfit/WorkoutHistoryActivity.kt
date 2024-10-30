package com.example.gayfit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.adapters.WorkoutAdapter
import com.example.gayfit.models.WorkoutCompleted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var workoutAdapter: WorkoutAdapter
    private val workouts = mutableListOf<WorkoutCompleted>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerViewWorkouts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        workoutAdapter = WorkoutAdapter(workouts)
        recyclerView.adapter = workoutAdapter

        fetchWorkouts()
    }

    private fun fetchWorkouts() {
        val userId = auth.currentUser?.uid ?: return

        Log.d("WorkoutHistory", "Fetching workouts for userId: $userId")

        db.collection("workouts")
            .whereEqualTo("userId", userId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d("WorkoutHistory", "Successfully fetched workouts: ${result.size()}")
                workouts.clear()
                for (document in result) {
                    Log.d("WorkoutHistory", "Document ID: ${document.id}, Data: ${document.data}")
                    val workout = document.toObject(WorkoutCompleted::class.java)
                    workouts.add(workout)
                }
                workoutAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutHistory", "Error fetching workouts", e)
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
