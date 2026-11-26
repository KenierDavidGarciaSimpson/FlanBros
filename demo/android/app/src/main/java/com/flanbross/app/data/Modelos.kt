package com.flanbross.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Respuesta(
    val exito: Boolean = false,
    val mensaje: String = "",
    val datos: JsonElement? = null,
)

@Serializable
data class Usuario(
    @SerialName("id_usuario") val idUsuario: Int = 0,
    @SerialName("id_entidad") val idEntidad: Int = 0,
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    @SerialName("nombre_rol") val rol: String = "",
    @SerialName("id_rol") val idRol: Int = 0,
)

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

@Serializable
data class ItemPedido(
    @SerialName("id_producto") val idProducto: Int,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double,
)

@Serializable
data class NuevoPedido(
    @SerialName("id_cliente") val idCliente: Int,
    @SerialName("direccion_entrega") val direccionEntrega: String,
    val observaciones: String? = null,
    @SerialName("metodo_pago") val metodoPago: String,
    val items: List<ItemPedido>,
    @SerialName("codigo_cupon") val codigoCupon: String? = null,
)

@Serializable
data class CuponValido(
    val codigo: String,
    @SerialName("descuento_pct") val porcentaje: Int,
)

@Serializable
data class PedidoCreado(
    @SerialName("id_pedido") val id: Int,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    @SerialName("estado_pedido") val estado: String = "",
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
)
