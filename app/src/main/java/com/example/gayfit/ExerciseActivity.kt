package com.example.gayfit

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.databinding.ActivityExerciseBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.WorkoutCompleted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gayfit.models.ExerciseCompleted
import com.example.gayfit.models.SetResult
import com.google.android.material.appbar.MaterialToolbar
import java.io.Serializable

class ExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseBinding
    private lateinit var exercises: List<Exercise>
    private var currentExerciseIndex = 0
    private var currentSetNumber = 1
    private lateinit var currentExercise: Exercise
    private val userInputs = mutableListOf<ExerciseCompleted>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var workoutTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        exercises = intent.getSerializableExtra("EXERCISES") as? List<Exercise> ?: emptyList()
        workoutTitle = intent.getStringExtra("WORKOUT_TITLE") ?: ""

        if (exercises.isEmpty()) {
            Toast.makeText(this, "Немає вправ для виконання", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentExercise = exercises[currentExerciseIndex]

        updateUI()

        binding.buttonNext.setOnClickListener {
            if (validateInput()) {
                saveUserInput()
                proceedToNextSetOrExercise()
            }
        }
    }

    private fun updateUI() {
        val toolbarExercise = findViewById<MaterialToolbar>(R.id.toolbarExercise)
        binding.textViewSetNumber.text = "Підхід $currentSetNumber з ${currentExercise.sets}"
        binding.editTextReps.text?.clear()
        binding.editTextWeight.text?.clear()
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

        // Знаходимо чи вже є записана ця вправа
        var exerciseCompleted = userInputs.find { it.name == currentExercise.name }
        if (exerciseCompleted == null) {
            exerciseCompleted = ExerciseCompleted(name = currentExercise.name)
            userInputs.add(exerciseCompleted)
        }

        exerciseCompleted.sets.add(SetResult(currentSetNumber, reps, weight))
    }

    private fun proceedToNextSetOrExercise() {
        if (currentSetNumber < currentExercise.sets) {
            currentSetNumber++
            updateUI()
        } else {
            // Переходимо до наступної вправи
            if (currentExerciseIndex < exercises.size - 1) {
                currentExerciseIndex++
                currentExercise = exercises[currentExerciseIndex]
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

        db.collection("workouts")
            .add(userWorkout)
            .addOnSuccessListener {
                Toast.makeText(this, "Тренування збережено", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
