package com.example.demo.Modelo;

import java.time.LocalDateTime;

public class MensajeContacto {

    public int id_mensaje;
    public String nombre;
    public String correo;
    public String telefono;
    public String asunto = "Otro motivo";
    public String mensaje;
    public LocalDateTime fecha_envio;

    public String estado = "sin_leer";
}
