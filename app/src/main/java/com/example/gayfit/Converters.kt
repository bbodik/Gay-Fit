// Converters.kt
package com.example.gayfit

import androidx.room.TypeConverter
import com.example.gayfit.models.ExerciseInWorkout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()


    @TypeConverter
    fun fromExerciseInWorkoutList(value: List<ExerciseInWorkout>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toExerciseInWorkoutList(value: String?): List<ExerciseInWorkout>? {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<ExerciseInWorkout>>() {}.type
        return gson.fromJson(value, listType)
    }
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(separator = ";")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(";")

        }
    }
}
