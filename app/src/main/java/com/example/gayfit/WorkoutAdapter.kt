package com.example.gayfit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.R
import com.example.gayfit.models.Workout
import java.text.SimpleDateFormat
import java.util.*

class WorkoutAdapter(private val workouts: List<Workout>) :
    RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val programTextView: TextView = itemView.findViewById(R.id.textViewProgram)
        val exercisesTextView: TextView = itemView.findViewById(R.id.textViewExercises)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = sdf.format(Date(workout.date))
        holder.dateTextView.text = date
        holder.programTextView.text = "Програма: ${workout.program}"
        holder.exercisesTextView.text = workout.exercises.joinToString("\n") { exercise ->
            "${exercise.name}: ${exercise.sets}x${exercise.reps}"
        }
    }

    override fun getItemCount(): Int = workouts.size
}
