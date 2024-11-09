package com.example.gayfit.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.R
import com.example.gayfit.models.ExerciseInWorkout
import com.example.gayfit.ui.ExerciseDetailActivity // Створимо це активність

class ExerciseAdapter(private val exercises: List<ExerciseInWorkout>) :
    RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseNameTextView: TextView = itemView.findViewById(R.id.textViewExerciseName)
        val setsAndRepsTextView: TextView = itemView.findViewById(R.id.textViewSetsAndReps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exerciseInWorkout = exercises[position]

        holder.exerciseNameTextView.text = exerciseInWorkout.exercise.name
        holder.setsAndRepsTextView.text = "Підходи: ${exerciseInWorkout.sets}, Повторення: ${exerciseInWorkout.reps}"

        // Обробка кліка для відкриття деталей вправи
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ExerciseDetailActivity::class.java).apply {
                putExtra("exercise_id", exerciseInWorkout.exercise.id) // Передаємо ID вправи для завантаження деталей
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = exercises.size
}
