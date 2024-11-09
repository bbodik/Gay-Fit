// StatisticsActivity.kt
package com.example.gayfit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityStatisticsBinding
import com.example.gayfit.models.WorkoutCompleted
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val workouts = mutableListOf<WorkoutCompleted>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarStatistics)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarStatistics.setNavigationOnClickListener { finish() }

        loadWorkoutData()
    }

    private fun loadWorkoutData() {
        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("workouts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                for (document in snapshot.documents) {
                    val workout = document.toObject(WorkoutCompleted::class.java)
                    workout?.let {
                        workouts.add(it)
                        Log.d("StatisticsActivity", "Додано workout: $it")
                    }
                }

                Log.d("StatisticsActivity", "Загальна кількість workouts: ${workouts.size}")

                withContext(Dispatchers.Main) {
                    setupCharts()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StatisticsActivity, "Помилка завантаження даних: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("StatisticsActivity", "Error loading workout data", e)
            }
        }
    }


    private fun setupCharts() {
        setupTotalVolumeChart()
        setupExerciseProgressChart()
        setupMuscleGroupDistributionChart()
        setupAverageWeightRepsChart()
    }

    private fun setupTotalVolumeChart() {
        try {
            val entries = workouts.mapIndexed { index, workout ->
                val workoutVolume = workout.exercises.sumByDouble { exercise ->
                    exercise.sets.sumByDouble { set -> set.weight * set.reps }
                }
                Log.d("StatisticsActivity", "Workout $index volume: $workoutVolume")
                BarEntry(index.toFloat(), workoutVolume.toFloat())
            }

            Log.d("StatisticsActivity", "TotalVolumeChart entries: $entries")

            val dataSet = BarDataSet(entries, "Загальний обсяг тренувань").apply {
                setColors(1) // Додаємо кольори для видимості
            }

            binding.totalVolumeChart.apply {
                data = BarData(dataSet)
                description = Description().apply { text = "Обсяг тренувань (вага x повторення)" }
                animateY(1000)
                invalidate()
            }
        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Error setting up TotalVolumeChart", e)
        }
    }


    private fun setupExerciseProgressChart() {
        // Приклад побудови прогресу для однієї вправи (можна розширити для вибору вправи)
        val exerciseEntries = mutableListOf<Entry>()
        workouts.forEachIndexed { index, workout ->
            workout.exercises.forEach { exercise ->
                if (exercise.name == "Жим") { // Замініть на вибрану вправу
                    val weight = exercise.sets.maxOf { it.weight }
                    exerciseEntries.add(Entry(index.toFloat(), weight.toFloat()))
                }
            }
        }

        val dataSet = LineDataSet(exerciseEntries, "Прогрес у Жимі")
        binding.exerciseProgressChart.apply {
            data = LineData(dataSet)
            description = Description().apply { text = "Прогрес у вибраній вправі" }
            animateX(1000)
            invalidate()
        }
    }

    private fun setupMuscleGroupDistributionChart() {
        val muscleGroupCount = mutableMapOf<String, Float>()
        workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                exercise.muscleGroups.forEach { group ->
                    if (muscleGroupCount.containsKey(group)) {
                        muscleGroupCount[group] = muscleGroupCount[group]!! + 1
                    } else {
                        muscleGroupCount[group] = 1f
                    }
                }
            }
        }

        val entries = muscleGroupCount.map { PieEntry(it.value, it.key) }
        val dataSet = PieDataSet(entries, "Розподіл навантажень")
        binding.muscleGroupDistributionChart.apply {
            data = PieData(dataSet)
            description = Description().apply { text = "Навантаження по групах м'язів" }
            animateY(1000)
            invalidate()
        }
    }


    private fun setupAverageWeightRepsChart() {
        val entries = mutableListOf<BarEntry>()
        workouts.forEachIndexed { index, workout ->
            val averageWeight = workout.exercises.flatMap { it.sets }.map { it.weight }.average()
            entries.add(BarEntry(index.toFloat(), averageWeight.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Середня вага на підхід")
        binding.averageWeightRepsChart.apply {
            data = BarData(dataSet)
            description = Description().apply { text = "Середня вага на підхід" }
            animateY(1000)
            invalidate()
        }
    }
}
