// CreateWorkoutActivity.kt
package com.example.gayfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateWorkoutActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val exercises = mutableListOf<Exercise>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Переконайтеся, що ви маєте відповідний layout файл
        setContentView(R.layout.activity_create_workout)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val titleEditText = findViewById<EditText>(R.id.editTextTitle)
        val addExerciseButton = findViewById<Button>(R.id.buttonAddExercise)
        val saveWorkoutButton = findViewById<Button>(R.id.buttonSaveWorkout)
        val exercisesLayout = findViewById<LinearLayout>(R.id.exercisesLayout)

        addExerciseButton.setOnClickListener {
            showAddExerciseDialog(exercisesLayout)
        }

        saveWorkoutButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            if (title.isNotEmpty() && exercises.isNotEmpty()) {
                val sharedWorkout = SharedWorkout(
                    creatorId = auth.currentUser?.uid ?: "",
                    title = title,
                    exercises = exercises,
                    userCount = 1 // Творець автоматично використовує тренування
                )
                db.collection("shared_workouts")
                    .add(sharedWorkout)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Тренування створено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddExerciseDialog(exercisesLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null)
        val exerciseNameEditText = dialogView.findViewById<EditText>(R.id.editTextExerciseName)
        val setsEditText = dialogView.findViewById<EditText>(R.id.editTextSets)
        val repsEditText = dialogView.findViewById<EditText>(R.id.editTextReps)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Додати вправу")
            .setView(dialogView)
            .setPositiveButton("Додати") { _, _ ->
                val name = exerciseNameEditText.text.toString().trim()
                val sets = setsEditText.text.toString().toIntOrNull() ?: 0
                val reps = repsEditText.text.toString().toIntOrNull() ?: 0

                if (name.isNotEmpty() && sets > 0 && reps > 0) {
                    val exercise = Exercise(name, sets, reps)
                    exercises.add(exercise)
                    val textView = TextView(this)
                    textView.text = "${exercise.name}: ${exercise.sets}x${exercise.reps}"
                    exercisesLayout.addView(textView)
                } else {
                    Toast.makeText(this, "Заповніть всі поля правильно", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.show()
    }
}
