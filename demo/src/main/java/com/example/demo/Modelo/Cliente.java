package com.example.demo.Modelo;

import java.time.LocalDateTime;

public class Cliente {

    public int id_cliente;
    public String nombre;
    public String apellido;
    public String correo;
    public String telefono;
    public String direccion;
    public LocalDateTime fecha_registro;

    public String estado_cliente = "activo";

    public int total_pedidos;

    public String nombreCompleto() {
        return (nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido);
    }
}
