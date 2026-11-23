package com.flanbross.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanbross.app.data.Producto
import com.flanbross.app.data.Res
import com.flanbross.app.data.Servicios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** CatalogoViewModel — productos disponibles (publico: no requiere sesion). */
class CatalogoViewModel : ViewModel() {

    private val _estado = MutableStateFlow<Cargable<List<Producto>>>(Cargable.Cargando)
    val estado: StateFlow<Cargable<List<Producto>>> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = Cargable.Cargando
            _estado.value = when (val r = Servicios.repositorio.productos()) {
                is Res.Ok -> Cargable.Listo(r.dato)
                is Res.Error -> Cargable.Error(r.mensaje)
            }
        }
    }

    fun producto(id: Int): Producto? =
        (estado.value as? Cargable.Listo)?.datos?.firstOrNull { it.id == id }
}
