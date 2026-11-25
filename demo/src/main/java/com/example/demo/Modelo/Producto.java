package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Producto — capa 1 (Modelo).
 *
 * <p>Los nombres de los campos son EXACTAMENTE los de la tabla {@code Producto}
 * de {@code flanbross.sql} y, a la vez, las claves JSON que ya consumen los JS del
 * frontend ({@code catalogo.js} lee {@code p.nombre_producto}, {@code p.precio},
 * {@code p.stock}...). Asi no hay que traducir nada en ninguna capa.</p>
 *
 * <p>Los campos son publicos a proposito: Jackson los serializa tal cual, sin
 * getters ni anotaciones, y esta capa queda sin un solo {@code import} de Spring.</p>
 *
 * <p>{@code emoji} y {@code badge} no son columnas de la base: son datos de
 * presentacion que usa la tarjeta del catalogo (respaldo cuando no hay imagen y
 * la etiqueta "Nuevo"/"Popular").</p>
 *
 * <p>Capa: Modelo. No sabe donde se guarda ni quien lo llama.</p>
 */
public class Producto {

    public int id_producto;
    public String nombre_producto;
    public String descripcion;

    /** DECIMAL(10,2). Se usa BigDecimal para no arrastrar errores de redondeo. */
    public BigDecimal precio;

    public int stock;
    public String categoria;
    public LocalDate fecha_vencimiento;

    /** disponible | inactivo | agotado (ENUM de la tabla). */
    public String estado_producto = "disponible";

    /** Nombre del archivo dentro de static/img/productos/ (NULL = usar emoji). */
    public String imagen;

    /** Presentacion: emoji de respaldo cuando no hay foto. */
    public String emoji;

    /** Presentacion: etiqueta de la tarjeta ("Nuevo", "Popular"). */
    public String badge;

    /** true si el producto puede venderse (mismo criterio que el WHERE del PHP). */
    public boolean estaDisponible() {
        return "disponible".equals(estado_producto);
    }

    /** true si hay existencias para la cantidad pedida. */
    public boolean hayStock(int cantidad) {
        return stock >= cantidad;
    }
}
