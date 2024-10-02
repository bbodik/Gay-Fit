// adapters/SharedWorkoutAdapter.kt
package com.example.gayfit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.R
import com.example.gayfit.models.SharedWorkout

class SharedWorkoutAdapter(
    private val workouts: List<SharedWorkout>,
    private val onWorkoutSelected: (SharedWorkout) -> Unit
) : RecyclerView.Adapter<SharedWorkoutAdapter.WorkoutViewHolder>() {

    inner class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val userCountTextView: TextView = itemView.findViewById(R.id.textViewUserCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.titleTextView.text = workout.title
        holder.userCountTextView.text = "Користувачів: ${workout.userCount}"
        holder.itemView.setOnClickListener {
            onWorkoutSelected(workout)
        }
    }

    override fun getItemCount(): Int = workouts.size
}
