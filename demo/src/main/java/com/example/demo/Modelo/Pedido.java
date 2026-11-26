package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido {

    public int id_pedido;
    public LocalDateTime fecha_pedido;

    public String estado_pedido = "pendiente";

    public BigDecimal total_pedido;
    public String observaciones;
    public int id_cliente;

    public String nombre;
    public String apellido;
    public String correo;
    public String numero_factura;
    public BigDecimal total_factura;
    public String estado_factura;

    public List<DetallePedido> detalles = new ArrayList<>();

    public boolean estaCerrado() {
        return "entregado".equals(estado_pedido) || "cancelado".equals(estado_pedido);
    }
}
