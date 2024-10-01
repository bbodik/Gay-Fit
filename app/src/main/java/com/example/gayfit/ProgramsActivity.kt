package com.example.gayfit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ProgramsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programs)

        val muscleMassButton = findViewById<Button>(R.id.buttonMuscleMass)
        val fatBurningButton = findViewById<Button>(R.id.buttonFatBurning)
        val enduranceButton = findViewById<Button>(R.id.buttonEndurance)

        muscleMassButton.setOnClickListener {
            startProgram("Нарощування м’язової маси")
        }

        fatBurningButton.setOnClickListener {
            startProgram("Спалювання жиру")
        }

        enduranceButton.setOnClickListener {
            startProgram("Підвищення витривалості")
        }
    }

    private fun startProgram(programName: String) {
        // Передай назву програми до StartWorkoutActivity або створити окрему логіку
        val intent = Intent(this, StartWorkoutActivity::class.java).apply {
            putExtra("PROGRAM_NAME", programName)
        }
        startActivity(intent)
    }
}
