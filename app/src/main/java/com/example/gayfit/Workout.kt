package com.example.gayfit.models

import java.io.Serializable

data class Workout(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val exercises: List<Exercise> = listOf(),
    val program: String = ""
)

data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0
):Serializable

data class SharedWorkout(
    var id: String = "",
    val creatorId: String = "",
    val title: String = "",
    val exercises: List<Exercise> = listOf(),
    var userCount: Long = 0
) : Serializable

data class WorkoutCompleted(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val exercises: List<ExerciseCompleted> = listOf(),
    val program: String = ""
) : Serializable

data class ExerciseCompleted(
    val name: String = "",
    val sets: MutableList<SetResult> = mutableListOf()
) : Serializable

data class SetResult(
    val setNumber: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0
) : Serializable
