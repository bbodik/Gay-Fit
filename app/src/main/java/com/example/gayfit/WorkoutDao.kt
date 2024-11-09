package com.example.gayfit
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity): Long

    @Transaction
    @Query("SELECT * FROM workouts WHERE isSynced = 0")
    suspend fun getUnsyncedWorkouts(): List<WorkoutWithExercises>


    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWorkout(workout: SavedWorkoutEntity)

    @Query("SELECT * FROM saved_workouts")
    suspend fun getSavedWorkouts(): List<SavedWorkoutEntity>

    @Query("SELECT * FROM saved_workouts WHERE id = :id")
    suspend fun getSavedWorkoutById(id: String): SavedWorkoutEntity?

    @Delete
    suspend fun deleteSavedWorkout(workout: SavedWorkoutEntity)

    @Transaction
    suspend fun insertWorkoutWithExercisesAndSets(
        workout: WorkoutEntity,
        exercises: List<ExerciseEntity>,
        sets: List<SetEntity>
    ) {
        val workoutId = insertWorkout(workout)
        for (exercise in exercises) {
            exercise.workoutId = workoutId
            val exerciseId = insertExercise(exercise)
            for (set in sets.filter { it.exerciseId == exercise.id }) {
                set.exerciseId = exerciseId
                insertSet(set)
            }
        }
    }
}



// WorkoutDao.kt
data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId",
        entity = ExerciseEntity::class
    )
    val exercises: List<ExerciseWithSets>
)


data class ExerciseWithSets(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
        entity = SetEntity::class
    )
    val sets: List<SetEntity>
)