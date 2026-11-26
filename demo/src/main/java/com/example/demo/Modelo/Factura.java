package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Factura {

    public int id_factura;

    public String numero_factura;

    public LocalDateTime fecha_factura;

    public BigDecimal subtotal;

    public BigDecimal impuestos;

    public BigDecimal total_factura;

    public String estado_factura = "pendiente";

    public int id_cliente;
    public int id_pedido;

    public String nombre;
    public String apellido;
    public String correo;
    public String estado_pedido;
}
