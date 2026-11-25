package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factura — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Factura} de {@code flanbross.sql}. Se genera sola al
 * confirmar el pedido: {@code numero_factura} es lo que {@code cuenta.js} muestra
 * como comprobante (antes lo devolvia {@code pedidos.php} en {@code datos.numero_factura}).</p>
 *
 * <p>{@code nombre}, {@code apellido}, {@code correo} y {@code estado_pedido} son
 * datos de los JOIN de {@code facturas.php}.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Factura {

    public int id_factura;

    /** Formato "FAC-2026-0007" (ano + consecutivo del pedido). */
    public String numero_factura;

    public LocalDateTime fecha_factura;

    /** Suma de las lineas, sin IVA ni domicilio. */
    public BigDecimal subtotal;

    /** IVA aplicado sobre el subtotal. */
    public BigDecimal impuestos;

    /** Lo que paga el cliente: subtotal + impuestos. */
    public BigDecimal total_factura;

    /** pendiente | pagada | anulada (ENUM de la tabla). */
    public String estado_factura = "pendiente";

    public int id_cliente;
    public int id_pedido;

    /** Calculados (JOIN con Cliente y Pedido). */
    public String nombre;
    public String apellido;
    public String correo;
    public String estado_pedido;
}
