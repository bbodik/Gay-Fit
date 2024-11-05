// ExerciseSelectionAdapter.kt
package com.example.gayfit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.R
import com.example.gayfit.models.Exercise

class ExerciseSelectionAdapter(
    private val exercises: List<Exercise>,
    private val selectedExercises: MutableList<Exercise>
) : RecyclerView.Adapter<ExerciseSelectionAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxSelectExercise)
        val exerciseNameTextView: TextView = itemView.findViewById(R.id.textViewExerciseName)
        val muscleGroupsTextView: TextView = itemView.findViewById(R.id.textViewMuscleGroups)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_selection, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.exerciseNameTextView.text = exercise.name
        holder.muscleGroupsTextView.text = "М'язові групи: ${exercise.muscleGroups.joinToString(", ")}"
        holder.checkBox.isChecked = selectedExercises.contains(exercise)

        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
            if (holder.checkBox.isChecked) {
                selectedExercises.add(exercise)
            } else {
                selectedExercises.remove(exercise)
            }
        }
    }

    override fun getItemCount(): Int = exercises.size
}
