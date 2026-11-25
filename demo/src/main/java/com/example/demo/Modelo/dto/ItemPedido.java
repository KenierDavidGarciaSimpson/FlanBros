package com.example.demo.Modelo.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ItemPedido — capa 1 (Modelo), objeto de transferencia.
 *
 * <p>Una linea del carrito tal como la manda {@code checkout.js}:</p>
 *
 * <pre>{ "id_producto": 1, "cantidad": 1, "precio_unitario": 42000 }</pre>
 *
 * <p>{@code precio_unitario} llega del navegador, asi que NO se usa para cobrar sin
 * mas: el repositorio vuelve a leer el precio del producto y compara. Igual que el
 * PHP, que solo lo usaba para el total.</p>
 *
 * <p>Capa: Modelo (DTO).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemPedido {

    public int id_producto;
    public int cantidad;
    public BigDecimal precio_unitario;

    /** Cantidad valida: al menos 1 unidad (un 0 o un negativo no es un pedido). */
    public boolean esValido() {
        return id_producto > 0 && cantidad > 0;
    }
}
