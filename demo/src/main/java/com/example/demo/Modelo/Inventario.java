package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Inventario {

    public int id_inventario;
    public int id_producto;
    public int cantidad_disponible;
    public int cantidad_minima = 5;
    public LocalDateTime fecha_actualizacion;

    public String estado_inventario = "normal";

    public String nombre_producto;
    public String categoria;
    public BigDecimal precio;

    public String calcularEstado() {
        if (cantidad_disponible <= 0) {
            return "critico";
        }
        if (cantidad_disponible <= cantidad_minima) {
            return "bajo";
        }
        return "normal";
    }
}
