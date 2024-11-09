package com.example.gayfit.ui

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.gayfit.R
import com.example.gayfit.databinding.ActivityExerciseDetailBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.MediaType
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExerciseDetailBinding
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exerciseNameTextView: TextView = binding.textViewExerciseName
        val muscleGroupsTextView: TextView = binding.textViewMuscleGroups
        val playerView: PlayerView = binding.playerView

        val exerciseId = intent.getStringExtra("exercise_id")
        val db = FirebaseFirestore.getInstance()
        db.collection("exercises").document(exerciseId ?: "").get()
            .addOnSuccessListener { document ->
                val exercise = document.toObject(Exercise::class.java)
                exercise?.let {
                    exerciseNameTextView.text = it.name
                    muscleGroupsTextView.text = "Групи м'язів: ${it.muscleGroups.joinToString(", ")}"
                    setupPlayer(it.mediaUrl, it.mediaType, playerView)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка завантаження даних вправи: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPlayer(mediaUrl: String, mediaType: MediaType, playerView: PlayerView) {
        when (mediaType) {
            MediaType.VIDEO -> {
                exoPlayer = ExoPlayer.Builder(this).build()
                playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }
            MediaType.IMAGE, MediaType.GIF -> {
                // Handle image/GIF display using Glide in layout
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        exoPlayer = null
    }
}