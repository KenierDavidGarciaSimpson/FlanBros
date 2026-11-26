package com.flanbross.app.ui

import androidx.lifecycle.ViewModel
import com.flanbross.app.data.CuponValido
import com.flanbross.app.data.PedidoCreado
import com.flanbross.app.data.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LineaCarrito(val producto: Producto, val cantidad: Int) {
    val subtotal: Long get() = producto.precio.toLong() * cantidad
}

class CarritoViewModel : ViewModel() {

    private val _lineas = MutableStateFlow<List<LineaCarrito>>(emptyList())
    val lineas: StateFlow<List<LineaCarrito>> = _lineas.asStateFlow()

    private val _cupon = MutableStateFlow<CuponValido?>(null)
    val cupon: StateFlow<CuponValido?> = _cupon.asStateFlow()

    fun aplicarCupon(cupon: CuponValido?) {
        _cupon.value = cupon
    }

    private val _ultimoPedido = MutableStateFlow<PedidoCreado?>(null)
    val ultimoPedido: StateFlow<PedidoCreado?> = _ultimoPedido.asStateFlow()

    fun enCarrito(idProducto: Int): Int =
        _lineas.value.firstOrNull { it.producto.id == idProducto }?.cantidad ?: 0

    fun libres(producto: Producto): Int = (producto.stock - enCarrito(producto.id)).coerceAtLeast(0)

    fun agregar(producto: Producto, cantidad: Int = 1): String? {
        if (producto.stock <= 0 || producto.estado != "disponible") return "${producto.nombre} está agotado"
        if (cantidad < 1) return "Elige una cantidad válida"
        val libres = libres(producto)
        if (libres <= 0) return "Ya tienes en el carrito todas las unidades disponibles"
        if (cantidad > libres) return "Solo puedes agregar $libres unidad(es) más"
        _lineas.update { actuales ->
            val posicion = actuales.indexOfFirst { it.producto.id == producto.id }
            if (posicion >= 0) {
                val actual = actuales[posicion]
                actuales.toMutableList().also {
                    it[posicion] = actual.copy(producto = producto, cantidad = actual.cantidad + cantidad)
                }
            } else {
                actuales + LineaCarrito(producto, cantidad)
            }
        }
        return null
    }

    fun sincronizar(catalogo: List<Producto>): List<String> {
        val avisos = mutableListOf<String>()
        _lineas.update { actuales ->
            actuales.mapNotNull { linea ->
                val p = catalogo.firstOrNull { it.id == linea.producto.id }
                when {
                    p == null || p.stock <= 0 || p.estado != "disponible" -> {
                        avisos += "${linea.producto.nombre} ya no está disponible y se quitó"
                        null
                    }
                    linea.cantidad > p.stock -> {
                        avisos += "De ${p.nombre} solo quedan ${p.stock}; se ajustó la cantidad"
                        LineaCarrito(p, p.stock)
                    }
                    else -> LineaCarrito(p, linea.cantidad)
                }
            }
        }
        return avisos
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
        _cupon.value = null
    }

    fun pedidoConfirmado(pedido: PedidoCreado) {
        _ultimoPedido.value = pedido
        vaciar()
    }

    fun subtotal(lineas: List<LineaCarrito>): Long = lineas.sumOf { it.subtotal }
}
