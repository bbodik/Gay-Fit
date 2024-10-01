package com.example.gayfit

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var reminderTimeTextView: TextView
    private lateinit var setReminderButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        reminderTimeTextView = findViewById(R.id.textViewReminderTime)
        setReminderButton = findViewById(R.id.buttonSetReminder)

        setReminderButton.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog(){
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
            }

            val timeInMillis = selectedCalendar.timeInMillis
            val currentTimeInMillis = System.currentTimeMillis()

            var delay = timeInMillis - currentTimeInMillis
            if(delay < 0){
                delay += TimeUnit.DAYS.toMillis(1)
            }

            val workRequest = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)

            reminderTimeTextView.text = "Нагадування встановлено на: $selectedHour:$selectedMinute"
        }, hour, minute, true)

        timePicker.show()
    }
}
