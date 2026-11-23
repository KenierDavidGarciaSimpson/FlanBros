package com.flanbross.app.ui

import androidx.lifecycle.ViewModel
import com.flanbross.app.data.PedidoCreado
import com.flanbross.app.data.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LineaCarrito(val producto: Producto, val cantidad: Int) {
    val subtotal: Long get() = producto.precio.toLong() * cantidad
}

/** CarritoViewModel — carrito en memoria, compartido por todas las pantallas de compra. */
class CarritoViewModel : ViewModel() {

    private val _lineas = MutableStateFlow<List<LineaCarrito>>(emptyList())
    val lineas: StateFlow<List<LineaCarrito>> = _lineas.asStateFlow()

    private val _ultimoPedido = MutableStateFlow<PedidoCreado?>(null)
    val ultimoPedido: StateFlow<PedidoCreado?> = _ultimoPedido.asStateFlow()

    /** Agrega unidades sin pasar del stock disponible. */
    fun agregar(producto: Producto, cantidad: Int = 1) {
        if (producto.stock <= 0) return
        _lineas.update { actuales ->
            val posicion = actuales.indexOfFirst { it.producto.id == producto.id }
            if (posicion >= 0) {
                val actual = actuales[posicion]
                val nueva = minOf(actual.cantidad + cantidad, producto.stock)
                actuales.toMutableList().also { it[posicion] = actual.copy(cantidad = nueva) }
            } else {
                actuales + LineaCarrito(producto, cantidad.coerceIn(1, producto.stock))
            }
        }
    }

    fun cambiarCantidad(idProducto: Int, cantidad: Int) {
        _lineas.update { actuales ->
            actuales.mapNotNull { linea ->
                if (linea.producto.id != idProducto) {
                    linea
                } else if (cantidad <= 0) {
                    null
                } else {
                    linea.copy(cantidad = minOf(cantidad, linea.producto.stock.coerceAtLeast(1)))
                }
            }
        }
    }

    fun quitar(idProducto: Int) = cambiarCantidad(idProducto, 0)

    fun vaciar() {
        _lineas.value = emptyList()
    }

    fun pedidoConfirmado(pedido: PedidoCreado) {
        _ultimoPedido.value = pedido
        vaciar()
    }

    fun subtotal(lineas: List<LineaCarrito>): Long = lineas.sumOf { it.subtotal }
}
