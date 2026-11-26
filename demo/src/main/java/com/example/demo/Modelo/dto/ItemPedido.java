package com.example.demo.Modelo.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemPedido {

    public int id_producto;
    public int cantidad;
    public BigDecimal precio_unitario;

    public boolean esValido() {
        return id_producto > 0 && cantidad > 0;
    }
}
