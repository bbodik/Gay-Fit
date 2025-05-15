// WorkoutSyncWorker.kt
package com.example.gayfit

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gayfit.models.ExerciseCompleted
import com.example.gayfit.models.SetResult
import com.example.gayfit.models.WorkoutCompleted
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// WorkoutSyncWorker.kt
class WorkoutSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WorkoutSyncWorker", "Worker started")

        FirebaseApp.initializeApp(applicationContext)
        val db = FirebaseFirestore.getInstance()
        val workoutDao = WorkoutDatabase.getDatabase(applicationContext).workoutDao()

        val unsyncedWorkouts = workoutDao.getUnsyncedWorkouts()

        Log.d("WorkoutSyncWorker", "Unsynced workouts: $unsyncedWorkouts")

        for (workoutWithExercises in unsyncedWorkouts) {
            val workoutEntity = workoutWithExercises.workout
            val exercisesCompleted = mutableListOf<ExerciseCompleted>()

            Log.d("WorkoutSyncWorker", "Exercises for workout ${workoutEntity.id}: ${workoutWithExercises.exercises}")

            for (exerciseWithSets in workoutWithExercises.exercises) {
                val exerciseEntity = exerciseWithSets.exercise
                val setsResult = exerciseWithSets.sets.map { setEntity ->
                    SetResult(
                        setNumber = setEntity.setNumber,
                        reps = setEntity.reps,
                        weight = setEntity.weight
                    )
                }
                val exerciseCompleted = ExerciseCompleted(
                    name = exerciseEntity.name,
                    muscleGroups = exerciseEntity.muscleGroups,
                    sets = setsResult.toMutableList()
                )
                exercisesCompleted.add(exerciseCompleted)
            }

            val workoutCompleted = WorkoutCompleted(
                id = "",
                userId = workoutEntity.userId,
                date = workoutEntity.date,
                program = workoutEntity.program,
                exercises = exercisesCompleted
            )

            Log.d("WorkoutSyncWorker", "Синхронізація тренування: $workoutCompleted")

            try {
                db.collection("workout_results")
                    .add(workoutCompleted)
                    .await()

                workoutEntity.isSynced = true
                workoutDao.updateWorkout(workoutEntity)

                Log.d("WorkoutSyncWorker", "Синхронізація тренування завершена для ID: ${workoutEntity.id}")

            } catch (e: Exception) {
                Log.e("WorkoutSyncWorker", "Помилка при синхронізації: ${e.message}", e)
                return Result.retry()
            }
        }

        return Result.success()
    }
}

