package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Producto {

    public int id_producto;
    public String nombre_producto;
    public String descripcion;

    public BigDecimal precio;

    public int stock;
    public String categoria;
    public LocalDate fecha_vencimiento;

    public String estado_producto = "disponible";

    public String imagen;

    public String emoji;

    public String badge;

    public boolean estaDisponible() {
        return "disponible".equals(estado_producto);
    }

    public boolean hayStock(int cantidad) {
        return stock >= cantidad;
    }
}
