// StatisticsActivity.kt
package com.example.gayfit

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityStatisticsBinding
import com.example.gayfit.models.WorkoutCompleted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val muscleGroupData = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        fetchWorkoutResults()
    }

    private fun fetchWorkoutResults() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("workout_results")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val workouts = result.toObjects(WorkoutCompleted::class.java)
                processWorkouts(workouts)
                displayStatistics()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processWorkouts(workouts: List<WorkoutCompleted>) {
        for (workout in workouts) {
            for (exercise in workout.exercises) {
                for (muscle in exercise.muscleGroups) {
                    val totalReps = exercise.sets.sumOf { it.reps }
                    muscleGroupData[muscle] = (muscleGroupData[muscle] ?: 0f) + totalReps

                }
            }
        }
    }

    private fun displayStatistics() {
        val entries = mutableListOf<BarEntry>()
        val muscleGroups = muscleGroupData.keys.toList()

        muscleGroupData.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value))
        }

        val dataSet = BarDataSet(entries, "Робота м'язів")
        val data = BarData(dataSet)

        val chart = binding.barChart
        chart.data = data

        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(muscleGroups)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        chart.invalidate() // Оновлюємо графік
    }
}
