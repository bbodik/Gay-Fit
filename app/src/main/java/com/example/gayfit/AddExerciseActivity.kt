package com.example.gayfit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
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
import com.google.firebase.storage.StorageMetadata
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

        // Оновлюємо початковий стан UI
        updateMediaSelectionUI(null)

        // Налаштування кнопки вибору медіа
        binding.buttonSelectMedia.setOnClickListener {
            checkAndRequestPermissions()
        }

        // Обробка кнопки збереження
        binding.buttonSaveExercise.setOnClickListener {
            saveExercise()
        }
    }


    private fun updateMediaSelectionUI(uri: Uri?) {
        if (uri != null) {
            // Оновлюємо текст кнопки
            binding.buttonSelectMedia.text = "Змінити медіафайл"

            // Показуємо назву файлу або тип медіа
            val fileName = getFileName(uri)
            binding.textViewSelectedFile.text = "Вибрано: $fileName"
            binding.textViewSelectedFile.visibility = View.VISIBLE

            when (mediaType) {
                MediaType.IMAGE -> {
                    // Якщо це зображення, можна показати превью
                    binding.imageViewPreview.setImageURI(uri)
                    binding.imageViewPreview.visibility = View.VISIBLE
                    binding.videoViewPreview.visibility = View.GONE
                }
                MediaType.VIDEO -> {
                    // Якщо це відео, можна показати превью відео
                    binding.videoViewPreview.setVideoURI(uri)
                    binding.videoViewPreview.visibility = View.VISIBLE
                    binding.imageViewPreview.visibility = View.GONE
                }
                MediaType.GIF -> {
                    // Обробляємо GIF як зображення
                    binding.imageViewPreview.setImageURI(uri)
                    binding.imageViewPreview.visibility = View.VISIBLE
                    binding.videoViewPreview.visibility = View.GONE
                }
            }
        } else {
            // Скидаємо UI до початкового стану
            binding.buttonSelectMedia.text = "Вибрати медіафайл"
            binding.textViewSelectedFile.visibility = View.GONE
            binding.imageViewPreview.visibility = View.GONE
            binding.videoViewPreview.visibility = View.GONE
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
    private fun getFileName(uri: Uri): String {
        // Спочатку пробуємо отримати ім'я файлу через курсор
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return cursor.getString(displayNameIndex)
                }
            }
        }

        // Якщо не вдалося отримати через курсор, пробуємо отримати з Uri
        uri.lastPathSegment?.let { return it }

        // Якщо все інше не спрацювало, повертаємо базову назву
        return "вибраний файл"
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
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val fileName = UUID.randomUUID().toString() + "." + extension

            // Створюємо reference з коректним шляхом
            val storageRef = storage.reference
                .child("exercise_media")
                .child(fileName)

            // Перевіряємо розмір файлу
            val fileSize = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
            if (fileSize > 100 * 1024 * 1024) { // 100MB limit
                throw Exception("File size exceeds limit")
            }

            // Встановлюємо метадані
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()

            // Завантажуємо файл
            val uploadTask = storageRef.putFile(uri, metadata)

            uploadTask
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    Log.d("Upload", "Progress: $progress%")
                }
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }
                .addOnFailureListener { e ->
                    Log.e("Upload", "Failed: ${e.message}", e)
                    Toast.makeText(
                        this@AddExerciseActivity,
                        "Upload failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (e: Exception) {
            Log.e("Upload", "Error: ${e.message}", e)
            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
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
                            mimeType.startsWith("image/gif") -> MediaType.GIF
                            mimeType.startsWith("image/") -> MediaType.IMAGE
                            mimeType.startsWith("video/") -> MediaType.VIDEO
                            else -> MediaType.IMAGE
                        }
                    }
                    // Оновлюємо UI після вибору файлу
                    updateMediaSelectionUI(mediaUri)
                    Toast.makeText(this, "Медіафайл вибрано успішно", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AddExerciseActivity", "Error accessing file: ${e.message}")
                    Toast.makeText(this, "Помилка доступу до файлу", Toast.LENGTH_SHORT).show()
                    mediaUri = null
                    updateMediaSelectionUI(null)
                }
            }
        }
    }

}