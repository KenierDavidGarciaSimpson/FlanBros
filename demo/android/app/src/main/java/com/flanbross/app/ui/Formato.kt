package com.flanbross.app.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de presentacion (pesos colombianos, fechas, estados). */
object Formato {
    private val colombia = Locale("es", "CO")
    private val patronFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /** 42000 -> "$42.000". El peso colombiano no usa centavos. */
    fun pesos(valor: Long): String = "$" + String.format(colombia, "%,d", valor)

    fun pesos(valor: Double): String = pesos(valor.toLong())

    /** "2026-11-22T10:30:00" -> "22/11/2026 10:30" (si no se puede leer, devuelve el texto tal cual). */
    fun fechaHora(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching { LocalDateTime.parse(iso.take(19)).format(patronFecha) }.getOrDefault(iso)
    }

    fun estadoPedido(estado: String): String = when (estado) {
        "pendiente" -> "Pendiente"
        "en_preparacion" -> "En preparación"
        "listo" -> "Listo"
        "entregado" -> "Entregado"
        "cancelado" -> "Cancelado"
        else -> estado
    }
}

/** Reglas de cobro del servidor: IVA 8 % sobre el subtotal, redondeado al peso (mitad hacia arriba). */
object Cobro {
    const val COSTO_DOMICILIO = 7000L

    fun iva(subtotal: Long): Long = (subtotal * 8 + 50) / 100

    fun total(subtotal: Long): Long = subtotal + iva(subtotal)
}
