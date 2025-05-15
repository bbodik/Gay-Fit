// CreateWorkoutExerciseAdapter.kt
package com.example.gayfit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.R
import com.example.gayfit.models.ExerciseInWorkout

class CreateWorkoutExerciseAdapter(
    private val exercises: List<ExerciseInWorkout>
) : RecyclerView.Adapter<CreateWorkoutExerciseAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseNameTextView: TextView = itemView.findViewById(R.id.textViewExerciseName)
        val exerciseDetailsTextView: TextView = itemView.findViewById(R.id.textViewExerciseDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_create_workout, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exerciseInWorkout = exercises[position]
        holder.exerciseNameTextView.text = exerciseInWorkout.exercise.name
        holder.exerciseDetailsTextView.text = "Сети: ${exerciseInWorkout.sets}, Повторення: ${exerciseInWorkout.reps}"
    }

    override fun getItemCount(): Int = exercises.size
}
