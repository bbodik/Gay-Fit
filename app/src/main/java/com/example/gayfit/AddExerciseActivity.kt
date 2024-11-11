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
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

class AddExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExerciseBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var mediaUri: Uri? = null
    private var mediaType: MediaType = MediaType.IMAGE

    private val REQUEST_CODE_SELECT_MEDIA = 100
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 101

    // Cloudinary налаштування
    private val cloudName = "dsb8rnybi" // Замініть на ваш Cloud Name
    private val uploadPreset = "5ykC_ZQaRFPu0wLHH4svbjdedK0" // Замініть на ваш Upload Preset

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

            // Використовуйте папку для організації файлів
            val storagePath = "exercises/$fileName"

            // Додайте логування шляху
            Log.d("StoragePath", "Path for upload: $storagePath")

            // Створіть запит до Cloudinary
            val client = OkHttpClient()

            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            if (bytes == null) {
                Log.e("Upload", "Не вдалося прочитати файл")
                runOnUiThread {
                    Toast.makeText(this, "Не вдалося прочитати файл", Toast.LENGTH_LONG).show()
                }
                return
            }

            // Переконайтеся, що MediaType не null
            val mediaTypeObj = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    bytes.toRequestBody(mediaTypeObj)
                )
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", "exercises") // Додаємо папку, якщо потрібно
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/upload")
                .post(requestBody)
                .build()

            Log.d("Upload", "Starting upload to Cloudinary...")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Upload", "Не вдалося завантажити файл: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this@AddExerciseActivity, "Завантаження не вдалося: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.e("Upload", "Не вдалося завантажити файл: ${response.message}. Body: $responseBody")
                        runOnUiThread {
                            Toast.makeText(this@AddExerciseActivity, "Завантаження не вдалося: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                        return
                    }

                    response.body?.let { responseBody ->
                        val responseString = responseBody.string()
                        Log.d("Upload", "Отримана відповідь: $responseString")

                        // Парсимо JSON відповідь
                        val gson = Gson()
                        val cloudinaryResponse = gson.fromJson(responseString, CloudinaryResponse::class.java)

                        // Переконайтеся, що secure_url присутній
                        val mediaUrl = cloudinaryResponse.secure_url
                        if (mediaUrl.isNullOrEmpty()) {
                            Log.e("Upload", "secure_url відсутній у відповіді")
                            runOnUiThread {
                                Toast.makeText(this@AddExerciseActivity, "Не вдалося отримати URL файлу", Toast.LENGTH_LONG).show()
                            }
                            return
                        }

                        Log.d("Upload", "Файл успішно завантажено. URL: $mediaUrl")

                        runOnUiThread {
                            onSuccess(mediaUrl)
                        }
                    } ?: run {
                        Log.e("Upload", "Пустий body у відповіді")
                        runOnUiThread {
                            Toast.makeText(this@AddExerciseActivity, "Пустий body у відповіді", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Upload", "Помилка: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
            }
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

    // Клас для парсингу відповіді Cloudinary
    data class CloudinaryResponse(
        val public_id: String,
        val version: Long,
        val signature: String,
        val width: Int,
        val height: Int,
        val format: String,
        val resource_type: String,
        val created_at: String,
        val tags: List<String>,
        val bytes: Int,
        val type: String,
        val etag: String,
        val placeholder: Boolean,
        val url: String,
        val secure_url: String
    )
}
