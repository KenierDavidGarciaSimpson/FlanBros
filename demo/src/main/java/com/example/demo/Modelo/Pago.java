package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Pago} de {@code flanbross.sql}. Se crea con el pedido
 * en estado "pendiente" y pasa a "aprobado" cuando el pedido se marca entregado
 * (misma regla que {@code pedidos.php}).</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Pago {

    public int id_pago;
    public LocalDateTime fecha_pago;
    public BigDecimal monto_pago;

    /** contraentrega | tarjeta | transferencia | billetera_digital (ENUM). */
    public String metodo_pago;

    /** pendiente | aprobado | rechazado (ENUM). */
    public String estado_pago = "pendiente";

    /** Referencia de la pasarela; NULL cuando es contraentrega. */
    public String referencia_pago;

    public int id_factura;

    /** Calculados (JOIN con Factura y Cliente) para la tabla del panel. */
    public String numero_factura;
    public BigDecimal total_factura;
    public String nombre;
    public String apellido;
    public String correo;
}
