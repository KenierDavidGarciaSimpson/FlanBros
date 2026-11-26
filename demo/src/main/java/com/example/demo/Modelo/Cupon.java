package com.example.demo.Modelo;

import java.math.BigDecimal;

public class Cupon {

    public String codigo;
    public int descuento_pct;
    public String descripcion;

    public boolean activo = true;

    public BigDecimal descuentoPara(BigDecimal subtotal) {
        if (subtotal == null) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(BigDecimal.valueOf(descuento_pct))
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
    }

    public String codigoNormalizado() {
        return codigo == null ? "" : codigo.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
