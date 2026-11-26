package com.example.demo.Modelo.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NuevoPedido {

    public Integer id_cliente;
    public List<ItemPedido> items;
    public String direccion_entrega;
    public String observaciones;

    public String metodo_pago = "contraentrega";

    public String codigo_cupon;

    @JsonIgnore
    public int descuento_pct;

    @JsonIgnore
    public String cupon_aplicado;

    public boolean estaCompleto() {
        return id_cliente != null && id_cliente > 0
                && items != null && !items.isEmpty()
                && direccion_entrega != null && !direccion_entrega.isBlank();
    }
}
