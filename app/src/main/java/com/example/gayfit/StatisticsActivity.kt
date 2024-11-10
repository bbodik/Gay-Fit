package com.example.gayfit

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityStatisticsBinding
import com.example.gayfit.models.WorkoutCompleted
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val workouts = mutableListOf<WorkoutCompleted>()
    private val uniqueExercises = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarStatistics)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarStatistics.setNavigationOnClickListener { finish() }

        showLoading(true)
        loadWorkoutData()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.chartsContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun loadWorkoutData() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Користувач не авторизований", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("workout_results")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                workouts.clear()
                uniqueExercises.clear()
                for (document in snapshot.documents) {
                    val workout = document.toObject(WorkoutCompleted::class.java)
                    if (workout != null) {
                        workout.id = document.id
                        workouts.add(workout)
                        workout.exercises.forEach { exercise ->
                            uniqueExercises.add(exercise.name)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (workouts.isEmpty()) {
                        Toast.makeText(this@StatisticsActivity, "Немає даних для відображення", Toast.LENGTH_LONG).show()
                    } else {
                        setupFilters()
                        setupCharts()
                    }
                    showLoading(false)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@StatisticsActivity, "Помилка завантаження даних: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("StatisticsActivity", "Error loading workout data", e)
                }
            }
        }
    }

    private fun setupFilters() {
        val exerciseNames = uniqueExercises.toList()
        val spinner = findViewById<Spinner>(R.id.exerciseSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, exerciseNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Правильне використання setOnItemSelectedListener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedExercise = exerciseNames[position]
                setupExerciseProgressChart(selectedExercise)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Можна залишити порожнім або додати дію за замовчуванням
            }
        }
    }

    private fun setupCharts() {
        setupTotalVolumeChart()
        setupMuscleGroupDistributionChart()
        setupAverageWeightRepsChart()
    }

    private fun setupTotalVolumeChart() {
        val entries = workouts.mapIndexed { index, workout ->
            val workoutVolume = workout.exercises.sumOf { exercise ->
                exercise.sets.sumOf { set -> set.weight * set.reps }
            }
            BarEntry(index.toFloat(), workoutVolume.toFloat())
        }

        val dataSet = BarDataSet(entries, "Загальний обсяг тренувань").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 10f
        }

        binding.totalVolumeChart.apply {
            data = BarData(dataSet)
            description = Description().apply {
                text = "Обсяг тренувань"
                textSize = 12f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(workouts.mapIndexed { i, _ -> "Тренування ${i + 1}" })
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupExerciseProgressChart(exerciseName: String) {
        val entries = workouts.mapIndexedNotNull { index, workout ->
            workout.exercises.find { it.name == exerciseName }?.let { exercise ->
                val maxWeight = exercise.sets.maxOfOrNull { it.weight } ?: 0.0
                Entry(index.toFloat(), maxWeight.toFloat())
            }
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, "Немає даних для вправи: $exerciseName", Toast.LENGTH_LONG).show()
            return
        }

        val dataSet = LineDataSet(entries, "Прогрес у $exerciseName").apply {
            color = ColorTemplate.MATERIAL_COLORS[1]
            valueTextSize = 10f
            lineWidth = 2f
            setCircleColor(ColorTemplate.MATERIAL_COLORS[1])
            circleRadius = 4f
            setDrawValues(false)
        }

        binding.exerciseProgressChart.apply {
            data = LineData(dataSet)
            description = Description().apply {
                text = "Прогрес у $exerciseName"
                textSize = 12f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(workouts.mapIndexed { i, _ -> "Тренування ${i + 1}" })
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupMuscleGroupDistributionChart() {
        val muscleGroupCount = workouts.flatMap { it.exercises }
            .flatMap { it.muscleGroups }
            .groupingBy { it }
            .eachCount()

        val entries = muscleGroupCount.map { PieEntry(it.value.toFloat(), it.key) }
        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "Розподіл навантажень").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 10f
            sliceSpace = 3f
            selectionShift = 5f
        }

        binding.muscleGroupDistributionChart.apply {
            data = PieData(dataSet)
            description = Description().apply {
                text = "Розподіл по групах м'язів"
                textSize = 12f
            }
            isDrawHoleEnabled = true
            holeRadius = 30f
            transparentCircleRadius = 35f
            animateY(1000)
            invalidate()
        }
    }

    private fun setupAverageWeightRepsChart() {
        val entries = workouts.mapIndexed { index, workout ->
            val averageWeight = workout.exercises.flatMap { it.sets }.map { it.weight }.average().toFloat()
            BarEntry(index.toFloat(), averageWeight)
        }

        val dataSet = BarDataSet(entries, "Середня вага на підхід").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 10f
        }

        binding.averageWeightRepsChart.apply {
            data = BarData(dataSet)
            description = Description().apply {
                text = "Середня вага на підхід"
                textSize = 12f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(workouts.mapIndexed { i, _ -> "Тренування ${i + 1}" })
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}
