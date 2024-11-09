package com.example.gayfit
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.Relation
import com.example.gayfit.models.ExerciseInWorkout

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var userId: String = "",
    var date: Long = 0L,
    var program: String = "",
    var isSynced: Boolean = false
)

@Entity(tableName = "saved_workouts")
data class SavedWorkoutEntity(
    @PrimaryKey val id: String,
    val title: String,
    val exercises: List<ExerciseInWorkout>
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutId"])]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var workoutId: Long = 0L,
    var name: String = "",
    var muscleGroups: List<String> = emptyList()
)


@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"])]
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var exerciseId: Long = 0L,
    var setNumber: Int = 0,
    var reps: Int = 0,
    var weight: Double = 0.0
)
