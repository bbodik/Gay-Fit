package com.example.gayfit.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
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
        val imageView: ImageView = binding.imageViewPreview // Переконайтеся, що цей ImageView присутній у layout

        val exerciseId = intent.getStringExtra("exercise_id")
        if (exerciseId.isNullOrEmpty()) {
            Toast.makeText(this, "Невірний ідентифікатор вправи", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("exercises").document(exerciseId).get()
            .addOnSuccessListener { document ->
                val exercise = document.toObject(Exercise::class.java)
                exercise?.let {
                    exerciseNameTextView.text = it.name
                    muscleGroupsTextView.text = "Групи м'язів: ${it.muscleGroups.joinToString(", ")}"
                    setupMedia(it.mediaUrl, it.mediaType, playerView, imageView)
                } ?: run {
                    Toast.makeText(this, "Вправа не знайдена", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка завантаження даних вправи: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupMedia(mediaUrl: String, mediaType: MediaType, playerView: PlayerView, imageView: ImageView) {
        when (mediaType) {
            MediaType.VIDEO -> {
                playerView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                exoPlayer = ExoPlayer.Builder(this).build()
                playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }
            MediaType.IMAGE, MediaType.GIF -> {
                playerView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                Glide.with(this)
                    .load(mediaUrl)
                    .into(imageView)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
