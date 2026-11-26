package com.example.demo.Modelo;

import java.math.BigDecimal;

public class DetallePedido {

    public int id_detalle;
    public int id_pedido;
    public int id_producto;
    public int cantidad;
    public BigDecimal precio_unitario;
    public BigDecimal subtotal;

    public String nombre_producto;

    public BigDecimal calcularSubtotal() {
        if (precio_unitario == null) {
            return BigDecimal.ZERO;
        }
        return precio_unitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
