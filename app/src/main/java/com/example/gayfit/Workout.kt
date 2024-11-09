// Workout.kt
package com.example.gayfit.models

import com.example.gayfit.SavedWorkoutEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Exercise(
    var id: String = "",
    var name: String = "",
    var guide: String = "",
    var description: String = "",
    var muscleGroups: List<String> = emptyList(),
    var mediaUrl: String = "",
    var mediaType: MediaType = MediaType.IMAGE,
    var createdBy: String = "",
    var timestamp: Long? = null // Додане поле timestamp
) : Serializable {

}

data class ExerciseInWorkout(
    var exercise: Exercise = Exercise(), // Ненульове поле з значенням за замовчуванням
    var sets: Int = 0,
    var reps: Int = 0,
    var weight: Float = 0f
) : Serializable {
    // Порожній конструктор без параметрів автоматично генерується
}

data class SharedWorkout(
    var id: String = "",
    var creatorId: String = "",
    var title: String = "",
    var exercises: List<ExerciseInWorkout> = emptyList(),
    var userCount: Int = 0
) : Serializable {
    // Порожній конструктор без параметрів

}

@IgnoreExtraProperties
data class WorkoutCompleted(
    var id: String = "",
    var userId: String = "",
    var date: Long = 0L,
    var program: String = "",
    var exercises: List<ExerciseCompleted> = listOf()
)

@IgnoreExtraProperties
data class ExerciseCompleted(
    var name: String = "",
    var muscleGroups: List<String> = listOf(),
    var sets: MutableList<SetResult> = mutableListOf()
)

@IgnoreExtraProperties
data class SetResult(
    var setNumber: Int = 0,
    var reps: Int = 0,
    var weight: Double = 0.0
)

enum class MediaType : Serializable {
    IMAGE,
    VIDEO,
    GIF
}
sealed class WorkoutItem {
    data class Online(val workout: SharedWorkout) : WorkoutItem()
    data class Saved(val workout: SavedWorkoutEntity) : WorkoutItem()
}
