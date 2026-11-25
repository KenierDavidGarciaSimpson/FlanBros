package com.example.demo.Modelo;

import java.math.BigDecimal;

/**
 * Cupon — capa 1 (Modelo).
 *
 * <p>En el diseno original los cupones NO estaban en la base de datos: eran un
 * arreglo fijo dentro de {@code cupones.php}. Pasan a este modelo para que el
 * controlador no tenga datos incrustados y para poder moverlos a una tabla el dia
 * de manana sin tocar el controlador.</p>
 *
 * <p>Los tres codigos son los mismos que ya usaba el frontend:
 * FLANBROSS10 (10%), BIENVENIDO (15%) y DULCE20 (20%).</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Cupon {

    public String codigo;
    public int descuento_pct;
    public String descripcion;

    /** Un cupon vencido se desactiva sin borrarlo. */
    public boolean activo = true;

    /** Descuento en dinero para un subtotal dado, redondeado como el PHP. */
    public BigDecimal descuentoPara(BigDecimal subtotal) {
        if (subtotal == null) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(BigDecimal.valueOf(descuento_pct))
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
    }

    /** El frontend compara el codigo en mayusculas y sin espacios. */
    public String codigoNormalizado() {
        return codigo == null ? "" : codigo.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
