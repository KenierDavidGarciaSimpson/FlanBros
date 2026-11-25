package com.example.demo.Modelo;

import java.math.BigDecimal;

/**
 * DetallePedido — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Detalle_Pedido} de {@code flanbross.sql}: una linea
 * por producto dentro de un pedido. El precio se copia en la linea a proposito,
 * para que un cambio de precio posterior no reescriba el historial.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class DetallePedido {

    public int id_detalle;
    public int id_pedido;
    public int id_producto;
    public int cantidad;
    public BigDecimal precio_unitario;
    public BigDecimal subtotal;

    /** Calculado (JOIN con Producto), para el detalle que muestra el panel. */
    public String nombre_producto;

    /** Subtotal de la linea, sin confiar en lo que venga del navegador. */
    public BigDecimal calcularSubtotal() {
        if (precio_unitario == null) {
            return BigDecimal.ZERO;
        }
        return precio_unitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
