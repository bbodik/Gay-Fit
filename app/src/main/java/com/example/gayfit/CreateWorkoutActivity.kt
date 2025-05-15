// CreateWorkoutActivity.kt
package com.example.gayfit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.adapters.CreateWorkoutExerciseAdapter
import com.example.gayfit.databinding.ActivityCreateWorkoutBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.ExerciseInWorkout
import com.example.gayfit.models.SharedWorkout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateWorkoutBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val exercisesInWorkout = mutableListOf<ExerciseInWorkout>()

    private lateinit var recyclerViewExercises: RecyclerView
    private lateinit var exerciseAdapter: CreateWorkoutExerciseAdapter

    private val REQUEST_SELECT_EXERCISES = 1
    private val REQUEST_ADD_EXERCISE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val titleEditText = binding.editTextTitle
        val selectExercisesButton = binding.buttonSelectExercises
        val saveWorkoutButton = binding.buttonSaveWorkout
        val addExerciseButton = binding.buttonAddExercise

        // Ініціалізація RecyclerView та адаптера
        recyclerViewExercises = binding.recyclerViewExercises
        exerciseAdapter = CreateWorkoutExerciseAdapter(exercisesInWorkout)
        recyclerViewExercises.layoutManager = LinearLayoutManager(this)
        recyclerViewExercises.adapter = exerciseAdapter

        selectExercisesButton.setOnClickListener {
            val intent = Intent(this, ExerciseSelectionActivity::class.java)
            startActivityForResult(intent, REQUEST_SELECT_EXERCISES)
        }

        // Обробник для кнопки створення нової вправи
        addExerciseButton.setOnClickListener {
            val intent = Intent(this, AddExerciseActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_EXERCISE)
        }

        saveWorkoutButton.setOnClickListener {
            val title = binding.editTextTitle.text.toString().trim()
            if (title.isNotEmpty() && exercisesInWorkout.isNotEmpty()) {
                val sharedWorkout = SharedWorkout(
                    creatorId = auth.currentUser?.uid ?: "",
                    title = title,
                    exercises = exercisesInWorkout,
                    userCount = 1 // Творець автоматично використовує тренування
                )
                db.collection("shared_workouts")
                    .add(sharedWorkout)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Тренування створено", Toast.LENGTH_SHORT).show()

                        // Запуск StartWorkoutActivity
                        val intent = Intent(this, StartWorkoutActivity::class.java).apply {
                            putExtra("WORKOUT_EXERCISES", ArrayList(exercisesInWorkout))
                        }
                        startActivity(intent)

                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Заповніть всі поля та додайте хоча б одну вправу", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        when (requestCode) {
            REQUEST_SELECT_EXERCISES -> {
                val selectedExercises = data.getSerializableExtra("SELECTED_EXERCISES") as? List<Exercise>
                if (selectedExercises != null) {
                    // Показуємо діалог для введення сетів і повторень для кожної вправи
                    for (exercise in selectedExercises) {
                        showSetRepsDialog(exercise)
                    }
                }
            }
            REQUEST_ADD_EXERCISE -> {
                val newExercise = data.getSerializableExtra("EXERCISE") as? Exercise
                if (newExercise != null) {
                    showSetRepsDialog(newExercise)
                }
            }
        }
    }

    private fun showSetRepsDialog(exercise: Exercise) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_reps, null)
        val setsEditText = dialogView.findViewById<android.widget.EditText>(R.id.editTextSets)
        val repsEditText = dialogView.findViewById<android.widget.EditText>(R.id.editTextReps)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Введіть сети та повторення для ${exercise.name}")
            .setView(dialogView)
            .setPositiveButton("ОК") { _, _ ->
                val sets = setsEditText.text.toString().toIntOrNull() ?: 0
                val reps = repsEditText.text.toString().toIntOrNull() ?: 0
                if (sets > 0 && reps > 0) {
                    val exerciseInWorkout = ExerciseInWorkout(
                        exercise = exercise,
                        sets = sets,
                        reps = reps
                    )
                    exercisesInWorkout.add(exerciseInWorkout)
                    exerciseAdapter.notifyItemInserted(exercisesInWorkout.size - 1)
                } else {
                    Toast.makeText(this, "Невірні значення сетів або повторень", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Відмінити", null)
            .create()
        dialog.show()
    }
}
