package com.flanbross.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanbross.app.data.Pedido
import com.flanbross.app.data.Res
import com.flanbross.app.data.Servicios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PedidosViewModel : ViewModel() {

    private val _estado = MutableStateFlow<Cargable<List<Pedido>>>(Cargable.Cargando)
    val estado: StateFlow<Cargable<List<Pedido>>> = _estado.asStateFlow()

    fun cargar(idCliente: Int) {
        viewModelScope.launch {
            _estado.value = Cargable.Cargando
            _estado.value = when (val r = Servicios.repositorio.pedidos(idCliente)) {
                is Res.Ok -> Cargable.Listo(r.dato)
                is Res.Error -> Cargable.Error(r.mensaje)
            }
        }
    }
}
