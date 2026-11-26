package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Domicilio {

    public int id_domicilio;
    public String direccion_entrega;
    public LocalDateTime fecha_entrega;

    public String estado_entrega = "pendiente";

    public BigDecimal costo_domicilio;
    public int id_pedido;

    public Integer id_domiciliario;

    public String estado_pedido;
    public BigDecimal total_pedido;
    public String nombre;
    public String apellido;
    public String telefono;
    public String numero_factura;
    public String nombre_domiciliario;
}
