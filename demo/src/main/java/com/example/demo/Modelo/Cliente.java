package com.example.demo.Modelo;

import java.time.LocalDateTime;

/**
 * Cliente — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Cliente} de {@code flanbross.sql}. Es quien compra:
 * {@code login.php} devuelve su {@code id_cliente} como {@code id_entidad} y
 * {@code checkout.js} lo manda de vuelta en el pedido.</p>
 *
 * <p>{@code total_pedidos} no es una columna: es el COUNT que el PHP calculaba con
 * el JOIN de {@code clientes.php} para pintar la tabla del panel. Se guarda aqui
 * como campo calculado para que el JSON salga igual.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Cliente {

    public int id_cliente;
    public String nombre;
    public String apellido;
    public String correo;
    public String telefono;
    public String direccion;
    public LocalDateTime fecha_registro;

    /** activo | inactivo (ENUM de la tabla). */
    public String estado_cliente = "activo";

    /** Calculado: cuantos pedidos tiene (JOIN con Pedido, como en clientes.php). */
    public int total_pedidos;

    /** Nombre completo listo para pintar. */
    public String nombreCompleto() {
        return (nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido);
    }
}
