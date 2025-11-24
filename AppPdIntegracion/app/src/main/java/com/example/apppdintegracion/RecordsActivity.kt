package com.example.apppdintegracion

import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Actividad para mostrar todos los registros de patentes.
 * Se presenta como una tabla con columnas para la patente, el usuario y el estado.
 */
class RecordsActivity : AppCompatActivity() {

    // ViewModel específico para esta pantalla, encargado de obtener todos los registros.
    private val viewModel: RecordsViewModel by viewModels()
    // Referencia al TableLayout definido en el archivo XML.
    private lateinit var recordsTableLayout: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        recordsTableLayout = findViewById(R.id.recordsTableLayout)

        // --- Observador de Datos del ViewModel ---
        lifecycleScope.launch {
            // Se suscribe a los cambios en `recordsData` del RecordsViewModel.
            viewModel.recordsData.collect { querySnapshot ->
                // Antes de añadir nuevas filas, limpia las anteriores para evitar duplicados.
                // Se elimina desde el índice 1 para no borrar la cabecera de la tabla.
                if (recordsTableLayout.childCount > 1) {
                    recordsTableLayout.removeViews(1, recordsTableLayout.childCount - 1)
                }

                // Itera sobre cada documento recibido de Firestore.
                querySnapshot?.documents?.forEach { document ->
                    // Crea una nueva fila (TableRow) para la tabla.
                    val tableRow = TableRow(this@RecordsActivity)

                    // Crea los TextViews para cada celda de la fila.
                    val patentTextView = TextView(this@RecordsActivity).apply {
                        text = document.id // El ID del documento es la patente.
                        setPadding(8, 8, 8, 8)
                        gravity = Gravity.START
                    }
                    val nameTextView = TextView(this@RecordsActivity).apply {
                        text = document.getString("usuario") ?: "no disponible"
                        setPadding(8, 8, 8, 8)
                        gravity = Gravity.START
                    }
                    val statusTextView = TextView(this@RecordsActivity).apply {
                        val isLinked = document.getBoolean("estado") ?: false
                        text = if (isLinked) "Vinculado" else "No Vinculado"
                        setPadding(8, 8, 8, 8)
                        gravity = Gravity.START
                    }

                    // Añade los TextViews (celdas) a la fila.
                    tableRow.addView(patentTextView)
                    tableRow.addView(nameTextView)
                    tableRow.addView(statusTextView)

                    // Finalmente, añade la fila completa a la tabla.
                    recordsTableLayout.addView(tableRow)
                }
            }
        }

        // Al crear la actividad, se le pide al ViewModel que inicie la obtención de todos los registros.
        viewModel.fetchAllRecords()
    }
}
