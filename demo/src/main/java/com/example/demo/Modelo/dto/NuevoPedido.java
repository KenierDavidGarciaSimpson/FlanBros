package com.example.demo.Modelo.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * NuevoPedido — capa 1 (Modelo), objeto de transferencia.
 *
 * <p>Cuerpo del POST a {@code pedidos.php}, tal cual lo arma {@code checkout.js}:</p>
 *
 * <pre>{ "id_cliente": 1, "items": [ { "id_producto": 1, "cantidad": 1,
 *   "precio_unitario": 42000 } ], "direccion_entrega": "Calle 5 #2-15, Bocagrande",
 *   "observaciones": "...", "metodo_pago": "contraentrega" }</pre>
 *
 * <p>{@code ignoreUnknown = true} para que un campo extra del navegador no tumbe el
 * pedido con un 400: el frontend ya manda alguno mas.</p>
 *
 * <p>Capa: Modelo (DTO).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NuevoPedido {

    public Integer id_cliente;
    public List<ItemPedido> items;
    public String direccion_entrega;
    public String observaciones;

    /** contraentrega | tarjeta | transferencia | billetera_digital. */
    public String metodo_pago = "contraentrega";

    /** Mismos obligatorios que validaba el PHP: cliente, items y direccion. */
    public boolean estaCompleto() {
        return id_cliente != null && id_cliente > 0
                && items != null && !items.isEmpty()
                && direccion_entrega != null && !direccion_entrega.isBlank();
    }
}
