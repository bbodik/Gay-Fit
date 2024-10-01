package com.example.gayfit.models

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
)
