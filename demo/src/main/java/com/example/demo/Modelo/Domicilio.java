package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domicilio — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Domicilio} de {@code flanbross.sql}. Se crea con el
 * pedido en estado "pendiente" y lo toma el domiciliario cuando el pedido pasa a
 * "listo" (regla de {@code pedidos.php}).</p>
 *
 * <p>Los campos de cliente, pedido y domiciliario son los JOIN de
 * {@code domicilios.php}, que es lo que pinta {@code domiciliario.html}.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Domicilio {

    public int id_domicilio;
    public String direccion_entrega;
    public LocalDateTime fecha_entrega;

    /** pendiente | en_camino | entregado | fallido (ENUM). */
    public String estado_entrega = "pendiente";

    public BigDecimal costo_domicilio;
    public int id_pedido;

    /** id del empleado domiciliario, o null si nadie lo ha tomado. */
    public Integer id_domiciliario;

    /** Calculados (JOIN con Pedido, Cliente, Factura y Empleado). */
    public String estado_pedido;
    public BigDecimal total_pedido;
    public String nombre;
    public String apellido;
    public String telefono;
    public String numero_factura;
    public String nombre_domiciliario;
}
