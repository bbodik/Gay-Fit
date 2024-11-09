package com.example.gayfit

import android.content.Context
import android.net.ConnectivityManager
import com.example.gayfit.ExerciseEntity
import com.example.gayfit.SetEntity
import com.example.gayfit.WorkoutEntity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.gayfit.databinding.ActivityExerciseBinding
import com.example.gayfit.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.Serializable
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExerciseActivity : AppCompatActivity() {

    private var _binding: ActivityExerciseBinding? = null
    private val binding get() = _binding!!

    private var exercises: List<ExerciseInWorkout> = emptyList()
    private var currentExerciseIndex = 0
    private var currentSetNumber = 1
    private lateinit var currentExerciseInWorkout: ExerciseInWorkout
    private val userInputs = mutableListOf<ExerciseCompleted>()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var workoutTitle: String = ""

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialData()
        setupClickListeners()
    }

    private fun setupInitialData() {
        try {
            @Suppress("UNCHECKED_CAST")
            exercises = intent.getSerializableExtra("EXERCISES") as? List<ExerciseInWorkout>
                ?: throw IllegalArgumentException("No exercises provided")

            workoutTitle = intent.getStringExtra("WORKOUT_TITLE")
                ?: throw IllegalArgumentException("No workout title provided")

            if (exercises.isEmpty()) {
                showError("Немає вправ для виконання")
                finish()
                return
            }

            currentExerciseInWorkout = exercises[currentExerciseIndex]
            updateUI()
        } catch (e: Exception) {
            showError("Помилка ініціалізації: ${e.message}")
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.buttonNext.setOnClickListener {
            if (validateInput()) {
                saveUserInput()
                proceedToNextSetOrExercise()
            }
        }
    }

    private fun updateUI() {
        val currentExercise = currentExerciseInWorkout.exercise
        binding.apply {
            textViewExerciseName.text = currentExercise.name
            // Замість використання ресурсу рядка
            textViewSetNumber.text = "Підхід $currentSetNumber з ${currentExerciseInWorkout.sets}"
            editTextReps.text?.clear()
            editTextWeight.text?.clear()
        }

        displayExerciseMedia(currentExercise)
    }

    private fun displayExerciseMedia(exercise: Exercise) {
        if (isNetworkAvailable()) {
        binding.apply {
            when (exercise.mediaType) {
                MediaType.IMAGE, MediaType.GIF -> {
                    imageViewExercise.visibility = View.VISIBLE
                    playerViewExercise.visibility = View.GONE
                    exoPlayer?.release()
                    exoPlayer = null

                    Glide.with(this@ExerciseActivity)
                        .load(exercise.mediaUrl)
                        .error(R.drawable.placeholder_exercise)
                        .into(imageViewExercise)
                }

                MediaType.VIDEO -> {
                    imageViewExercise.visibility = View.GONE
                    playerViewExercise.visibility = View.VISIBLE
                    setupPlayer(exercise.mediaUrl, playerViewExercise)
                }
            }
        }
        } else {
            Toast.makeText(this, "Немає інтернет-з'єднання. Медіа недоступне.", Toast.LENGTH_SHORT).show()

        }
    }

    private fun setupPlayer(mediaUrl: String, playerView: PlayerView) {
        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playerView.player = this
            setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
            prepare()
            playWhenReady = true
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun validateInput(): Boolean {
        val repsText = binding.editTextReps.text.toString()
        val weightText = binding.editTextWeight.text.toString()

        return when {
            repsText.isEmpty() -> {
                showError("Будь ласка, введіть кількість повторень")
                false
            }
            weightText.isEmpty() -> {
                showError("Будь ласка, введіть вагу")
                false
            }
            else -> try {
                repsText.toInt()
                weightText.toDouble()
                true
            } catch (e: NumberFormatException) {
                showError("Будь ласка, введіть коректні числові значення")
                false
            }
        }
    }

    private fun saveUserInput() {
        val reps = binding.editTextReps.text.toString().toInt()
        val weight = binding.editTextWeight.text.toString().toDouble()
        val currentExercise = currentExerciseInWorkout.exercise

        if (binding.checkBoxApplyToAllSets.isChecked) {
            // Додаємо дані для всіх підходів цієї вправи
            val exerciseCompleted = ExerciseCompleted(
                name = currentExercise.name,
                muscleGroups = currentExercise.muscleGroups
            )

            for (setNumber in 1..currentExerciseInWorkout.sets) {
                exerciseCompleted.sets.add(SetResult(setNumber, reps, weight))
            }

            userInputs.add(exerciseCompleted)
        } else {
            // Зберігаємо дані тільки для поточного підходу
            val exerciseCompleted = userInputs.find { it.name == currentExercise.name } ?: ExerciseCompleted(
                name = currentExercise.name,
                muscleGroups = currentExercise.muscleGroups
            ).also { userInputs.add(it) }

            exerciseCompleted.sets.add(SetResult(currentSetNumber, reps, weight))
        }
    }



    private fun proceedToNextSetOrExercise() {
        if (binding.checkBoxApplyToAllSets.isChecked) {
            // Якщо обрано застосувати до всіх підходів, переходимо до наступної вправи
            if (currentExerciseIndex < exercises.size - 1) {
                currentExerciseIndex++
                currentExerciseInWorkout = exercises[currentExerciseIndex]
                currentSetNumber = 1
                updateUI()
            } else {
                saveWorkout()
            }
        } else {
            if (currentSetNumber >= currentExerciseInWorkout.sets) {
                // Якщо всі підходи для поточної вправи завершено, переходимо до наступної вправи
                if (currentExerciseIndex < exercises.size - 1) {
                    currentExerciseIndex++
                    currentExerciseInWorkout = exercises[currentExerciseIndex]
                    currentSetNumber = 1
                    updateUI()
                } else {
                    saveWorkout()
                }
            } else {
                // Переходимо до наступного підходу
                currentSetNumber++
                updateUI()
            }
        }
    }



    private fun saveWorkout() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

                val workout = WorkoutEntity(
                    userId = userId,
                    date = System.currentTimeMillis(),
                    program = workoutTitle
                )

                val workoutId = WorkoutDatabase.getDatabase(this@ExerciseActivity)
                    .workoutDao()
                    .insertWorkout(workout)

                Log.d("ExerciseActivity", "Збережено тренування з ID: $workoutId")

                for (exerciseCompleted in userInputs) {
                    val exerciseEntity = ExerciseEntity(
                        workoutId = workoutId,
                        name = exerciseCompleted.name,
                        muscleGroups = exerciseCompleted.muscleGroups
                    )

                    val exerciseId = WorkoutDatabase.getDatabase(this@ExerciseActivity)
                        .workoutDao()
                        .insertExercise(exerciseEntity)

                    Log.d("ExerciseActivity", "Збережено вправу з ID: $exerciseId для тренування ID: $workoutId")

                    for (set in exerciseCompleted.sets) {
                        val setId = WorkoutDatabase.getDatabase(this@ExerciseActivity)
                            .workoutDao()
                            .insertSet(
                                SetEntity(
                                    exerciseId = exerciseId,
                                    setNumber = set.setNumber,
                                    reps = set.reps,
                                    weight = set.weight
                                )
                            )
                        Log.d("ExerciseActivity", "Збережено підхід з ID: $setId для вправи ID: $exerciseId")
                    }
                }

                // Додаємо лог перед запуском воркера для синхронізації
                Log.d("ExerciseActivity", "Тренування збережено локально, готуємо синхронізацію")

                // Запуск воркера для синхронізації з Firebase
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncWorkRequest = OneTimeWorkRequestBuilder<WorkoutSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(this@ExerciseActivity).enqueue(syncWorkRequest)

                withContext(Dispatchers.Main) {
                    showSuccess("Тренування збережено локально")
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Помилка при збереженні тренування: ${e.message}")
                }
            }
        }
    }





    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}