package com.example.gayfit

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.Workout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StartWorkoutActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Приклад вправ, їх можна зробити динамічними або завантажувати з бази даних
    private val exercises = listOf(
        Exercise("Присідання", 3, 12),
        Exercise("Віджимання", 3, 10),
        Exercise("Планка", 3, 60)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_workout)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val programName = intent.getStringExtra("PROGRAM_NAME") ?: "Власне"

        val saveWorkoutButton = findViewById<Button>(R.id.buttonSaveWorkout)

        // Завантаження вправ залежно від програми
        val exercises = loadExercisesForProgram(programName)

        exercises.forEach { exercise ->
            val checkBox = CheckBox(this).apply {
                text = "${exercise.name} - ${exercise.sets} сетів по ${exercise.reps} повторень"
                id = View.generateViewId()
            }
            findViewById<LinearLayout>(R.id.exercisesLayout).addView(checkBox)
        }


        saveWorkoutButton.setOnClickListener {
            val selectedExercises = mutableListOf<Exercise>()
            val layout = findViewById<LinearLayout>(R.id.exercisesLayout)
            for(i in 0 until layout.childCount){
                val child = layout.getChildAt(i)
                if(child is CheckBox && child.isChecked){
                    val exercise = exercises[i]
                    selectedExercises.add(exercise)
                }
            }

            if(selectedExercises.isNotEmpty()){
                val workout = Workout(
                    userId = auth.currentUser?.uid ?: "",
                    date = System.currentTimeMillis(),
                    exercises = selectedExercises,
                    program = "Власне"
                )
                db.collection("workouts")
                    .add(workout)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Тренування збережено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Виберіть хоча б одну вправу", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadExercisesForProgram(programName: String): List<Exercise> {
        return when(programName){
            "Нарощування м’язової маси" -> listOf(
                Exercise("Жим лежачи", 4, 8),
                Exercise("Присідання", 4, 10),
                Exercise("Тяга верхнього блоку", 4, 8)
            )
            "Спалювання жиру" -> listOf(
                Exercise("Кардіо 30 хв", 1, 1),
                Exercise("Віджимання", 3, 15),
                Exercise("Присідання", 3, 20)
            )
            "Підвищення витривалості" -> listOf(
                Exercise("Біг 5 км", 1, 1),
                Exercise("Планка", 3, 90),
                Exercise("Випади", 3, 15)
            )
            else -> listOf(
                Exercise("Присідання", 3, 12),
                Exercise("Віджимання", 3, 10),
                Exercise("Планка", 3, 60)
            )
        }
    }
}
