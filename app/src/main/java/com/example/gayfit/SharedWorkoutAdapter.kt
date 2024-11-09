// SharedWorkoutAdapter.kt
package com.example.gayfit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gayfit.R
import com.example.gayfit.models.SharedWorkout
import com.example.gayfit.SavedWorkoutEntity
import com.example.gayfit.models.WorkoutItem

class SharedWorkoutAdapter(
    private var items: List<WorkoutItem>,
    private val onWorkoutSelected: (SharedWorkout) -> Unit,
    private val onSavedWorkoutSelected: (SavedWorkoutEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ONLINE = 1
        private const val VIEW_TYPE_SAVED = 2
    }

    fun updateItems(newItems: List<WorkoutItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WorkoutItem.Online -> VIEW_TYPE_ONLINE
            is WorkoutItem.Saved -> VIEW_TYPE_SAVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_ONLINE) {
            val view = inflater.inflate(R.layout.item_shared_workout, parent, false)
            OnlineWorkoutViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_saved_workout, parent, false)
            SavedWorkoutViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WorkoutItem.Online -> {
                (holder as OnlineWorkoutViewHolder).bind(item.workout)
            }
            is WorkoutItem.Saved -> {
                (holder as SavedWorkoutViewHolder).bind(item.workout)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class OnlineWorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val userCountTextView: TextView = itemView.findViewById(R.id.textViewUserCount)

        fun bind(workout: SharedWorkout) {
            titleTextView.text = workout.title
            userCountTextView.text = "Користувачів: ${workout.userCount}"
            itemView.setOnClickListener { onWorkoutSelected(workout) }
        }
    }

    inner class SavedWorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)

        fun bind(workout: SavedWorkoutEntity) {
            titleTextView.text = workout.title
            itemView.setOnClickListener { onSavedWorkoutSelected(workout) }
        }
    }
}
