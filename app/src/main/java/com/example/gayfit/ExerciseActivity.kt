// ExerciseActivity.kt
package com.example.gayfit

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gayfit.databinding.ActivityExerciseBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.ExerciseCompleted
import com.example.gayfit.models.ExerciseInWorkout
import com.example.gayfit.models.MediaType
import com.example.gayfit.models.SetResult
import com.example.gayfit.models.WorkoutCompleted
import com.google.android.exoplayer2.ExoPlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseBinding
    private lateinit var exercises: List<ExerciseInWorkout>
    private var currentExerciseIndex = 0
    private var currentSetNumber = 1
    private lateinit var currentExerciseInWorkout: ExerciseInWorkout
    private val userInputs = mutableListOf<ExerciseCompleted>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var workoutTitle: String = ""
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        exercises = intent.getSerializableExtra("EXERCISES") as? List<ExerciseInWorkout> ?: emptyList()
        workoutTitle = intent.getStringExtra("WORKOUT_TITLE") ?: ""

        if (exercises.isEmpty()) {
            Toast.makeText(this, "Немає вправ для виконання", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentExerciseInWorkout = exercises[currentExerciseIndex]

        updateUI()

        binding.buttonNext.setOnClickListener {
            if (validateInput()) {
                saveUserInput()
                proceedToNextSetOrExercise()
            }
        }
    }

    private fun updateUI() {
        val currentExercise = currentExerciseInWorkout.exercise
        binding.textViewExerciseName.text = currentExercise.name
        binding.textViewSetNumber.text = "Підхід $currentSetNumber з ${currentExerciseInWorkout.sets}"
        binding.editTextReps.text?.clear()
        binding.editTextWeight.text?.clear()

        // Відображення медіа-контенту
        displayExerciseMedia(currentExercise)
    }

    private fun displayExerciseMedia(exercise: Exercise) {
        when (exercise.mediaType) {
            MediaType.IMAGE, MediaType.GIF -> {
                binding.imageViewExercise.visibility = View.VISIBLE
                binding.videoViewExercise.visibility = View.GONE
                Glide.with(this)
                    .load(exercise.mediaUrl)
                    .into(binding.imageViewExercise)
            }
            MediaType.VIDEO -> {
                binding.imageViewExercise.visibility = View.GONE
                binding.videoViewExercise.visibility = View.VISIBLE
                exoPlayer = ExoPlayer.Builder(this).build()
                binding.videoViewExercise.player = exoPlayer
                val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(Uri.parse(exercise.mediaUrl))
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }
        }
    }

    private fun validateInput(): Boolean {
        val repsText = binding.editTextReps.text.toString()
        val weightText = binding.editTextWeight.text.toString()

        if (repsText.isEmpty() || weightText.isEmpty()) {
            Toast.makeText(this, "Будь ласка, введіть кількість повторень та вагу", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveUserInput() {
        val reps = binding.editTextReps.text.toString().toInt()
        val weight = binding.editTextWeight.text.toString().toDouble()
        val currentExercise = currentExerciseInWorkout.exercise

        // Знаходимо чи вже є записана ця вправа
        var exerciseCompleted = userInputs.find { it.name == currentExercise.name }
        if (exerciseCompleted == null) {
            exerciseCompleted = ExerciseCompleted(name = currentExercise.name, muscleGroups = currentExercise.muscleGroups)
            userInputs.add(exerciseCompleted)
        }

        exerciseCompleted.sets.add(SetResult(currentSetNumber, reps, weight))
    }

    private fun proceedToNextSetOrExercise() {
        if (currentSetNumber < currentExerciseInWorkout.sets) {
            currentSetNumber++
            updateUI()
        } else {
            // Зупиняємо відео перед переходом
            exoPlayer?.release()
            exoPlayer = null

            // Переходимо до наступної вправи
            if (currentExerciseIndex < exercises.size - 1) {
                currentExerciseIndex++
                currentExerciseInWorkout = exercises[currentExerciseIndex]
                currentSetNumber = 1
                updateUI()
            } else {
                // Тренування завершено
                saveWorkout()
            }
        }
    }

    private fun saveWorkout() {
        val userWorkout = WorkoutCompleted(
            userId = auth.currentUser?.uid ?: "",
            date = System.currentTimeMillis(),
            exercises = userInputs,
            program = workoutTitle
        )

        db.collection("workout_results")
            .add(userWorkout)
            .addOnSuccessListener {
                Toast.makeText(this, "Тренування збережено", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}
