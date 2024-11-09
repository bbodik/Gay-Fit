// AddExerciseActivity.kt
package com.example.gayfit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gayfit.databinding.ActivityAddExerciseBinding
import com.example.gayfit.models.Exercise
import com.example.gayfit.models.MediaType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExerciseBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var mediaUri: Uri? = null
    private var mediaType: MediaType = MediaType.IMAGE

    private val REQUEST_CODE_SELECT_MEDIA = 100
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Перевірка автентифікації
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Перевірка та запит дозволу
        checkReadExternalStoragePermission()

        // Налаштування вибору медіа
        binding.buttonSelectMedia.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            startActivityForResult(Intent.createChooser(intent, "Виберіть медіа"), REQUEST_CODE_SELECT_MEDIA)
        }

        // Обробка кнопки збереження
        binding.buttonSaveExercise.setOnClickListener {
            saveExercise()
        }
    }

    private fun checkReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Дозвіл надано
            } else {
                Toast.makeText(this, "Потрібен дозвіл для доступу до файлів", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveExercise() {
        val name = binding.editTextExerciseName.text.toString().trim()
        val muscleGroupsText = binding.editTextMuscleGroups.text.toString().trim()
        val muscleGroups = muscleGroupsText.split(",").map { it.trim() }
        val description = binding.editTextDescription.text.toString().trim()

        if (name.isEmpty() || muscleGroups.isEmpty()) {
            Toast.makeText(this, "Заповніть всі обов'язкові поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (mediaUri != null) {
            uploadMediaFile(mediaUri!!) { mediaUrl ->
                saveExerciseToFirestore(name, muscleGroups, description, mediaUrl)
            }
        } else {
            saveExerciseToFirestore(name, muscleGroups, description, "")
        }
    }

    private fun uploadMediaFile(uri: Uri, onSuccess: (String) -> Unit) {
        // Перевірка доступу до файлу
        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Не вдалося отримати доступ до вибраного файлу", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaRef = storage.reference.child("exercise_media/${UUID.randomUUID()}")
        mediaRef.putFile(uri)
            .addOnSuccessListener {
                mediaRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка завантаження медіа: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveExerciseToFirestore(
        name: String,
        muscleGroups: List<String>,
        description: String,
        mediaUrl: String
    ) {
        val exerciseId = UUID.randomUUID().toString()
        val exercise = Exercise(
            id = exerciseId,
            name = name,
            guide = mediaUrl, // Використовуйте mediaUrl як гайд (можливо, це не найкраща практика)
            description = description,
            muscleGroups = muscleGroups,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            createdBy = auth.currentUser?.uid ?: ""
        )

        db.collection("exercises").document(exerciseId)
            .set(exercise)
            .addOnSuccessListener {
                val resultIntent = Intent().apply {
                    putExtra("EXERCISE", exercise)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка збереження вправи: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_MEDIA && resultCode == Activity.RESULT_OK) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Потрібен дозвіл для доступу до файлів", Toast.LENGTH_SHORT).show()
                return
            }

            mediaUri = data?.data
            Log.d("AddExerciseActivity", "Selected media URI: $mediaUri")
            if (mediaUri != null) {
                // Перевірка доступу до файлу
                try {
                    val inputStream = contentResolver.openInputStream(mediaUri!!)
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Не вдалося отримати доступ до вибраного файлу", Toast.LENGTH_SHORT).show()
                    return
                }

                // Визначення типу медіа
                val mimeType = contentResolver.getType(mediaUri!!)
                mediaType = if (mimeType?.startsWith("video") == true) {
                    MediaType.VIDEO
                } else {
                    MediaType.IMAGE
                }
                binding.textViewSelectedMedia.text = "Медіа вибрано: ${mediaUri?.lastPathSegment}"
            }
        }
    }
}
