package com.flanbross.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Sobre que usa TODA la API del servidor: { exito, mensaje, datos }.
 * `datos` es un objeto o una lista segun el endpoint (y [] en los errores), por eso se guarda
 * como JsonElement y cada llamada lo decodifica al tipo que espera.
 */
@Serializable
data class Respuesta(
    val exito: Boolean = false,
    val mensaje: String = "",
    val datos: JsonElement? = null,
)

@Serializable
data class Usuario(
    @SerialName("id_usuario") val idUsuario: Int = 0,
    /** id_cliente si el rol es "cliente"; id_empleado si es personal. */
    @SerialName("id_entidad") val idEntidad: Int = 0,
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    @SerialName("nombre_rol") val rol: String = "",
    @SerialName("id_rol") val idRol: Int = 0,
)

/** Cuerpo de `datos` en /api/login, /api/registro y /api/sesion. */
@Serializable
data class DatosSesion(val usuario: Usuario)

@Serializable
data class Producto(
    @SerialName("id_producto") val id: Int,
    @SerialName("nombre_producto") val nombre: String,
    val descripcion: String? = null,
    val precio: Double = 0.0,
    val stock: Int = 0,
    val categoria: String? = null,
    @SerialName("estado_producto") val estado: String = "disponible",
    /** Ruta relativa al servidor (p. ej. /img/productos/torta.png); puede venir vacia. */
    val imagen: String? = null,
    val emoji: String? = null,
    val badge: String? = null,
)

@Serializable
data class LineaPedido(
    @SerialName("nombre_producto") val nombre: String = "",
    val cantidad: Int = 0,
    val subtotal: Double = 0.0,
)

@Serializable
data class Pedido(
    @SerialName("id_pedido") val id: Int,
    @SerialName("fecha_pedido") val fecha: String? = null,
    @SerialName("estado_pedido") val estado: String = "",
    @SerialName("total_pedido") val subtotal: Double = 0.0,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    @SerialName("total_factura") val totalFactura: Double? = null,
    @SerialName("estado_factura") val estadoFactura: String? = null,
    val detalles: List<LineaPedido> = emptyList(),
)

// ---- Peticion de un pedido nuevo (POST /api/pedidos) ----

@Serializable
data class ItemPedido(
    @SerialName("id_producto") val idProducto: Int,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double,
)

@Serializable
data class NuevoPedido(
    /** El servidor lo ignora y usa el cliente de la sesion; se envia por compatibilidad. */
    @SerialName("id_cliente") val idCliente: Int,
    @SerialName("direccion_entrega") val direccionEntrega: String,
    val observaciones: String? = null,
    @SerialName("metodo_pago") val metodoPago: String,
    val items: List<ItemPedido>,
)

@Serializable
data class PedidoCreado(
    @SerialName("id_pedido") val id: Int,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    @SerialName("estado_pedido") val estado: String = "",
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
)
