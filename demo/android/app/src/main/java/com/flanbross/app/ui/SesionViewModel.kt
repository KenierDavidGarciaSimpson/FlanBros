package com.flanbross.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanbross.app.data.Servicios
import com.flanbross.app.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoSesion {
    data object Verificando : EstadoSesion
    data class Activa(val usuario: Usuario) : EstadoSesion
    data object Cerrada : EstadoSesion
}

class SesionViewModel : ViewModel() {

    private val repositorio = Servicios.repositorio
    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Verificando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    init {
        Servicios.http.alExpirar = { _estado.value = EstadoSesion.Cerrada }
        verificar()
    }

    fun verificar() {
        viewModelScope.launch {
            val usuario = repositorio.sesionActual()
            _estado.value = if (usuario != null) EstadoSesion.Activa(usuario) else EstadoSesion.Cerrada
        }
    }

    fun iniciar(usuario: Usuario) {
        _estado.value = EstadoSesion.Activa(usuario)
    }

    fun cerrar() {
        viewModelScope.launch {
            repositorio.cerrarSesion()
            _estado.value = EstadoSesion.Cerrada
        }
    }
}
