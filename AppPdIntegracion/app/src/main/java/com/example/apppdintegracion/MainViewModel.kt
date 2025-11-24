package com.example.apppdintegracion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la MainActivity.
 * Se encarga de la lógica de negocio, como realizar consultas a Firebase,
 * y expone los resultados a la UI de forma segura y consciente del ciclo de vida.
 */
class MainViewModel : ViewModel() {

    // Instancia de la base de datos de Firestore.
    private val db = Firebase.firestore

    // StateFlow privado para contener los datos de la patente. Solo el ViewModel puede modificarlo.
    private val _patentData = MutableStateFlow<Map<String, Any>?>(null)
    // StateFlow público y de solo lectura. La UI observa este Flow para recibir actualizaciones.
    val patentData = _patentData.asStateFlow()

    /**
     * Realiza una consulta a la colección "patentes" de Firestore para obtener un documento.
     * @param patent El ID del documento a buscar (la patente en minúsculas).
     */
    fun queryPatent(patent: String) {
        // Se lanza una corrutina en el ámbito del ViewModel, que se cancela automáticamente si el ViewModel se destruye.
        viewModelScope.launch {
            db.collection("patentes").document(patent)
                .get() // Intenta obtener el documento.
                .addOnSuccessListener { document ->
                    // Se ejecuta si la consulta es exitosa (incluso si el documento no existe).
                    if (document != null && document.exists()) {
                        // El documento existe, se actualiza el StateFlow con sus datos.
                        _patentData.value = document.data
                    } else {
                        // La consulta fue exitosa pero el documento no fue encontrado.
                        Log.d("MainViewModel", "No se encontró un documento para la patente: $patent")
                        _patentData.value = null // Se notifica a la UI que no hay datos.
                    }
                }
                .addOnFailureListener { exception ->
                    // Se ejecuta si hay un error de red, de permisos, etc.
                    Log.e("MainViewModel", "Error al obtener el documento para la patente: $patent", exception)
                    _patentData.value = null // Se notifica a la UI que hubo un error.
                }
        }
    }
}
