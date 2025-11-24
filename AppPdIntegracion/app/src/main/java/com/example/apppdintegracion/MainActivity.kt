package com.example.apppdintegracion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imageView: ImageView
    private lateinit var plateTextView: TextView
    private lateinit var nameTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var manualPatentEditText: EditText

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            imageView.setImageBitmap(it)
            val inputImage = InputImage.fromBitmap(it, 0)
            processImage(inputImage)
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                imageView.setImageURI(it)
                val inputImage = InputImage.fromFilePath(this, it)
                processImage(inputImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                takePicture.launch(null)
            } else {
                Toast.makeText(this, "El permiso de cámara es necesario para escanear patentes", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Configuración de la Toolbar ---
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Prototipo1"

        imageView = findViewById(R.id.imageView)
        plateTextView = findViewById(R.id.plateTextView)
        nameTextView = findViewById(R.id.nameTextView)
        statusTextView = findViewById(R.id.statusTextView)
        manualPatentEditText = findViewById(R.id.manualPatentEditText)

        val searchManualButton: Button = findViewById(R.id.searchManualButton)
        searchManualButton.setOnClickListener {
            val patentText = manualPatentEditText.text.toString()
            if (patentText.length == 6) {
                val lowercasePatent = patentText.lowercase()
                plateTextView.text = lowercasePatent
                nameTextView.text = "Buscando..."
                statusTextView.text = ""
                viewModel.queryPatent(lowercasePatent)
            } else {
                Toast.makeText(this, "La patente debe tener 6 caracteres", Toast.LENGTH_SHORT).show()
            }
        }

        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        val galleryButton: Button = findViewById(R.id.galleryButton)
        galleryButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        val viewRecordsButton: Button = findViewById(R.id.viewRecordsButton)
        viewRecordsButton.setOnClickListener {
            val intent = Intent(this, RecordsActivity::class.java)
            startActivity(intent)
        }

        val addRecordButton: Button = findViewById(R.id.addRecordButton)
        addRecordButton.setOnClickListener {
            val intent = Intent(this, AddRecordActivity::class.java)
            startActivity(intent)
        }

        lifecycleScope.launch {
            viewModel.patentData.collect { data ->
                if (data != null) {
                    nameTextView.text = data["usuario"] as? String ?: "no disponible"
                    val isLinked = data["estado"] as? Boolean ?: false
                    statusTextView.text = if (isLinked) "Vinculado" else "No Vinculado"
                } else {
                    if (plateTextView.text.isNotEmpty()) {
                        nameTextView.text = "no disponible"
                        statusTextView.text = ""
                    }
                }
            }
        }
    }

    /**
     * Infla el menú de opciones (definido en main_menu.xml) en la Toolbar.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Maneja los clics en los ítems del menú de la Toolbar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_guide -> {
                showGuideDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle("Guía de Funciones")
            .setMessage(
                "- **Ingresar patente y Buscar:** Escribe una patente de 6 caracteres y pulsa Buscar para consultar su información.\n\n"
                        + "- **Escanear Patente:** Usa la cámara para enfocar una patente y que la app la reconozca automáticamente.\n\n"
                        + "- **Abrir Galería:** Selecciona una foto de tu dispositivo que contenga una patente para que la app la analice.\n\n"
                        + "- **Examinar Registros:** Muestra una lista completa de todas las patentes guardadas en la base de datos.\n\n"
                        + "- **Agregar Registro:** Abre un formulario para añadir una nueva patente a la base de datos."
            )
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePicture.launch(null)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Se necesita acceso a la cámara para leer la patente", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun processImage(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                var found = false
                for (block in visionText.textBlocks) {
                    val patentRegex = "([a-z]{3}[0-9]{3})|([a-z]{4}[0-9]{2})".toRegex(RegexOption.IGNORE_CASE)
                    val cleanedText = block.text.replace("[\\s-]".toRegex(), "")
                    val patentMatch = patentRegex.find(cleanedText)?.value

                    if (patentMatch != null) {
                        val lowercasePatent = patentMatch.lowercase()
                        plateTextView.text = lowercasePatent
                        nameTextView.text = "Buscando..."
                        statusTextView.text = ""
                        viewModel.queryPatent(lowercasePatent)
                        found = true
                        break
                    }
                }
                if (!found) {
                    Toast.makeText(this, "No se encontró una patente válida", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al procesar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }
}
