package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Pedido} de {@code flanbross.sql}, mas las columnas
 * que traian los JOIN de {@code pedidos.php} ({@code numero_factura},
 * {@code total_factura}, {@code estado_factura} y los datos del cliente) y su lista
 * de lineas.</p>
 *
 * <p>{@code total_pedido} es la suma de las lineas SIN IVA ni domicilio; el valor
 * que paga el cliente esta en {@link Factura#total_factura}. Igual que en el PHP
 * original: no se "arregla" aqui para no cambiar los numeros que ya mostraba la
 * vista de cuenta.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Pedido {

    public int id_pedido;
    public LocalDateTime fecha_pedido;

    /** pendiente | en_preparacion | listo | entregado | cancelado (ENUM). */
    public String estado_pedido = "pendiente";

    public BigDecimal total_pedido;
    public String observaciones;
    public int id_cliente;

    /** Calculados (JOIN con Cliente y Factura). */
    public String nombre;
    public String apellido;
    public String correo;
    public String numero_factura;
    public BigDecimal total_factura;
    public String estado_factura;

    /** Lineas del pedido (tabla Detalle_Pedido). */
    public List<DetallePedido> detalles = new ArrayList<>();

    /** true si el pedido ya no admite cambios de estado normales. */
    public boolean estaCerrado() {
        return "entregado".equals(estado_pedido) || "cancelado".equals(estado_pedido);
    }
}
