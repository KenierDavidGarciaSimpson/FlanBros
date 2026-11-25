package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inventario — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Inventario} de {@code flanbross.sql}. En el diseno
 * original el stock se guarda dos veces (aqui y en {@code Producto.stock}); el PHP
 * mantenia los dos sincronizados en cada movimiento. Aqui se hace igual, y queda
 * anotado como deuda conocida del diseno original, no como decision nueva.</p>
 *
 * <p>{@code nombre_producto}, {@code categoria} y {@code precio} son datos del JOIN
 * que usaba {@code inventario.php} para pintar la tabla.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Inventario {

    public int id_inventario;
    public int id_producto;
    public int cantidad_disponible;
    public int cantidad_minima = 5;
    public LocalDateTime fecha_actualizacion;

    /** normal | bajo | critico (ENUM de la tabla). */
    public String estado_inventario = "normal";

    /** Calculados (JOIN con Producto). */
    public String nombre_producto;
    public String categoria;
    public BigDecimal precio;

    /**
     * Recalcula el estado a partir de la cantidad, con los mismos cortes que
     * {@code inventario.php}: 0 o menos = critico, hasta el minimo = bajo.
     */
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
