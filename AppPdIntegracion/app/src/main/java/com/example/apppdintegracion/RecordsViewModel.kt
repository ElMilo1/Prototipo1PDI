package com.example.apppdintegracion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la RecordsActivity.
 * Su única responsabilidad es obtener TODOS los documentos de la colección "patentes"
 * y exponerlos a la UI para que puedan ser mostrados en una tabla.
 */
class RecordsViewModel : ViewModel() {

    // Instancia de la base de datos de Firestore.
    private val db = Firebase.firestore

    // StateFlow privado para contener la lista de todos los registros. Solo el ViewModel puede modificarlo.
    private val _recordsData = MutableStateFlow<QuerySnapshot?>(null)
    // StateFlow público y de solo lectura que la UI observará.
    val recordsData = _recordsData.asStateFlow()

    /**
     * Realiza una consulta para obtener todos los documentos de la colección "patentes".
     */
    fun fetchAllRecords() {
        viewModelScope.launch {
            db.collection("patentes")
                .get() // Obtiene todos los documentos de la colección.
                .addOnSuccessListener { result ->
                    // Se ejecuta si la consulta es exitosa.
                    if (result.isEmpty) {
                        // Es una buena práctica registrar si la consulta no devuelve nada.
                        Log.w("RecordsViewModel", "La consulta fue exitosa, pero no se encontraron documentos en la colección 'patentes'.")
                    }
                    _recordsData.value = result // Actualiza el StateFlow con el resultado (incluso si está vacío).
                }
                .addOnFailureListener { exception ->
                    // Se ejecuta si hay un error de red o de permisos.
                    Log.e("RecordsViewModel", "Error al obtener la colección 'patentes'", exception)
                    _recordsData.value = null // Notifica a la UI que hubo un error.
                }
        }
    }
}
