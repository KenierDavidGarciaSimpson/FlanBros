package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago {

    public int id_pago;
    public LocalDateTime fecha_pago;
    public BigDecimal monto_pago;

    public String metodo_pago;

    public String estado_pago = "pendiente";

    public String referencia_pago;

    public int id_factura;

    public String numero_factura;
    public BigDecimal total_factura;
    public String nombre;
    public String apellido;
    public String correo;
}
