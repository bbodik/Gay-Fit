package com.example.gayfit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // Налаштування кнопки вибору медіа
        binding.buttonSelectMedia.setOnClickListener {
            checkAndRequestPermissions()
        }

        // Обробка кнопки збереження
        binding.buttonSaveExercise.setOnClickListener {
            saveExercise()
        }
    }

    private fun checkAndRequestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

            if (permissions.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }) {
                openMediaPicker()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_CODE_READ_EXTERNAL_STORAGE
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                openMediaPicker()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(this)
                        .setTitle("Потрібен дозвіл")
                        .setMessage("Для вибору медіафайлу потрібен доступ до сховища.")
                        .setPositiveButton("Надати дозвіл") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                REQUEST_CODE_READ_EXTERNAL_STORAGE
                            )
                        }
                        .setNegativeButton("Відміна", null)
                        .show()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_CODE_READ_EXTERNAL_STORAGE
                    )
                }
            }
        }
    }

    private fun openMediaPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        startActivityForResult(Intent.createChooser(intent, "Виберіть медіа"), REQUEST_CODE_SELECT_MEDIA)
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
            guide = mediaUrl,
            description = description,
            muscleGroups = muscleGroups,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            createdBy = auth.currentUser?.uid ?: ""
        )

        db.collection("exercises").document(exerciseId)
            .set(exercise)
            .addOnSuccessListener {
                Toast.makeText(this, "Вправу успішно додано", Toast.LENGTH_SHORT).show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Дозволи надано, відкриваємо picker
                    openMediaPicker()
                } else {
                    Toast.makeText(this, "Для вибору медіафайлу потрібні дозволи", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_MEDIA && resultCode == Activity.RESULT_OK) {
            mediaUri = data?.data
            if (mediaUri != null) {
                try {
                    contentResolver.getType(mediaUri!!)?.let { mimeType ->
                        mediaType = when {
                            mimeType.startsWith("image/") -> MediaType.IMAGE
                            mimeType.startsWith("video/") -> MediaType.VIDEO
                            else -> MediaType.IMAGE
                        }
                    }
                    // Можна додати відображення вибраного файлу
                    Toast.makeText(this, "Медіафайл вибрано успішно", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AddExerciseActivity", "Error accessing file: ${e.message}")
                    Toast.makeText(this, "Помилка доступу до файлу", Toast.LENGTH_SHORT).show()
                    mediaUri = null
                }
            }
        }
    }
}