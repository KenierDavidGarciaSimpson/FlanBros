package com.example.demo.Modelo;

import java.time.LocalDateTime;

public class Usuario {

    public int id_usuario;
    public int id_rol;

    public Integer id_cliente;

    public Integer id_empleado;

    public String correo;

    public String contrasena_hash;

    public String contrasena_demo;

    public String token_reset;
    public LocalDateTime token_expira;

    public boolean esDeCliente() {
        return id_cliente != null;
    }
}
