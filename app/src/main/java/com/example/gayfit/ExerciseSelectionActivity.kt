// ExerciseSelectionActivity.kt
package com.example.gayfit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gayfit.adapters.ExerciseSelectionAdapter

import com.example.gayfit.databinding.ActivityExerciseSelectionBinding
import com.example.gayfit.models.Exercise
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseSelectionBinding
    private lateinit var db: FirebaseFirestore
    private val exercises = mutableListOf<Exercise>()
    private val selectedExercises = mutableListOf<Exercise>()
    private lateinit var adapter: ExerciseSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        adapter = ExerciseSelectionAdapter(exercises, selectedExercises)
        binding.recyclerViewExercises.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewExercises.adapter = adapter

        binding.buttonConfirmSelection.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("SELECTED_EXERCISES", selectedExercises as ArrayList<Exercise>)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        binding.buttonConfirmSelection.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("SELECTED_EXERCISES", ArrayList(selectedExercises))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                fetchExercises(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Нічого не робимо
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Нічого не робимо
            }
        })

        fetchExercises("")
    }

    private fun fetchExercises(query: String) {
        db.collection("exercises")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                exercises.clear()
                for (document in result) {
                    val exercise = document.toObject(Exercise::class.java)
                    exercises.add(exercise)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
