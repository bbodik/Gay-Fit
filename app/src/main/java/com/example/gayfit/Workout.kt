// Workout.kt
package com.example.gayfit.models

import java.io.Serializable

data class Workout(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val exercises: List<ExerciseInWorkout> = listOf(),
    val program: String = ""
)

data class Exercise(
    var id: String = "",
    var name: String = "",
    var guide: String = "",
    var description: String = "",
    var muscleGroups: List<String> = emptyList(),
    var mediaUrl: String = "",
    var mediaType: MediaType = MediaType.IMAGE,
    var createdBy: String = ""
) : Serializable {
    // Порожній конструктор без параметрів
    constructor() : this("", "", "", "", emptyList(), "", MediaType.IMAGE, "")
}

data class ExerciseInWorkout(
    var exercise: Exercise = Exercise(), // Ненульове поле з значенням за замовчуванням
    var sets: Int = 0,
    var reps: Int = 0,
    var weight: Float = 0f
) : Serializable {
    // Порожній конструктор без параметрів автоматично генерується
}
enum class MediaType : Serializable {
    IMAGE,
    VIDEO,
    GIF
}
data class SharedWorkout(
    var id: String = "",
    var creatorId: String = "",
    var title: String = "",
    var exercises: List<ExerciseInWorkout> = emptyList(),
    var userCount: Int = 0
) : Serializable {
    // Порожній конструктор без параметрів
    constructor() : this("", "", "", emptyList(), 0)
}

data class WorkoutCompleted(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val exercises: List<ExerciseCompleted> = listOf(),
    val program: String = ""
) : Serializable

data class ExerciseCompleted(
    val name: String = "",
    val sets: MutableList<SetResult> = mutableListOf(),
    val muscleGroups: List<String> = listOf(),

) : Serializable

data class SetResult(
    val setNumber: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0
) : Serializable
