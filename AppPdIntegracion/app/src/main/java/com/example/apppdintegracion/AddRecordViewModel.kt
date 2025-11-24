package com.example.apppdintegracion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la AddRecordActivity.
 * Su responsabilidad es tomar los datos de una nueva patente y guardarlos en Firestore.
 * Utiliza un SharedFlow para comunicar el resultado de la operación (éxito o fracaso) a la UI.
 */
class AddRecordViewModel : ViewModel() {

    // Instancia de la base de datos de Firestore.
    private val db = Firebase.firestore

    // SharedFlow para emitir eventos de un solo uso. A diferencia de StateFlow, no guarda el último estado.
    // Es perfecto para eventos como "Guardado con éxito" que solo deben mostrarse una vez.
    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult = _saveResult.asSharedFlow() // Versión pública de solo lectura para la UI.

    /**
     * Guarda un nuevo registro de patente en la colección "patentes".
     * El ID del documento será la propia patente en minúsculas para asegurar consistencia.
     *
     * @param patent La matrícula del vehículo (se guardará en minúsculas).
     * @param user El nombre del usuario asociado.
     * @param status El estado de vinculación (true para vinculado, false para no vinculado).
     */
    fun saveRecord(patent: String, user: String, status: Boolean) {
        viewModelScope.launch {
            // Crea un mapa (similar a un diccionario) con los datos a guardar.
            val record = hashMapOf(
                "patente" to patent, // Se guarda la patente también como un campo dentro del documento.
                "usuario" to user,
                "estado" to status
            )

            // Usa .set() en lugar de .add() para especificar un ID de documento personalizado.
            // Si un documento con este ID ya existe, será sobreescrito. Si no, será creado.
            db.collection("patentes").document(patent)
                .set(record)
                .addOnSuccessListener {
                    Log.d("AddRecordViewModel", "Documento guardado con éxito! ID: $patent")
                    // Emite un evento de éxito (true) a través del SharedFlow.
                    viewModelScope.launch { _saveResult.emit(true) }
                }
                .addOnFailureListener { e ->
                    Log.w("AddRecordViewModel", "Error al escribir el documento", e)
                    // Emite un evento de fracaso (false) si la operación falla.
                    viewModelScope.launch { _saveResult.emit(false) }
                }
        }
    }
}
