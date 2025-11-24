package com.example.apppdintegracion

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class AddRecordActivity : AppCompatActivity() {

    private val viewModel: AddRecordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_record)

        // --- Inicialización de Vistas ---
        val patentEditText: EditText = findViewById(R.id.addPatentEditText)
        val userEditText: EditText = findViewById(R.id.addUserEditText)
        val statusSwitch: SwitchMaterial = findViewById(R.id.addStatusSwitch)
        val saveButton: Button = findViewById(R.id.saveRecordButton)

        // --- Listener para el botón de Guardar ---
        saveButton.setOnClickListener {
            val patent = patentEditText.text.toString().lowercase()
            val user = userEditText.text.toString()
            val status = statusSwitch.isChecked

            // Validaciones básicas antes de guardar.
            if (patent.length != 6) {
                Toast.makeText(this, "La patente debe tener 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (user.isBlank()) {
                Toast.makeText(this, "El campo de usuario no puede estar vacío.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Deshabilita el botón para evitar múltiples clics mientras se guarda.
            saveButton.isEnabled = false
            // Llama al ViewModel para que guarde los datos.
            viewModel.saveRecord(patent, user, status)
        }

        // --- Observador del Resultado ---
        lifecycleScope.launch {
            viewModel.saveResult.collect { success ->
                if (success) {
                    Toast.makeText(this@AddRecordActivity, "Registro guardado con éxito!", Toast.LENGTH_SHORT).show()
                    finish() // Cierra la actividad y vuelve a la pantalla principal.
                } else {
                    Toast.makeText(this@AddRecordActivity, "Error al guardar el registro.", Toast.LENGTH_SHORT).show()
                    saveButton.isEnabled = true // Rehabilita el botón si hubo un error.
                }
            }
        }
    }
}
