package com.example.demo.Modelo;

import java.time.LocalDateTime;

/**
 * MensajeContacto — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Mensaje_Contacto} de {@code flanbross.sql}: lo que
 * llega del formulario de contacto ({@code contacto.php}).</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class MensajeContacto {

    public int id_mensaje;
    public String nombre;
    public String correo;
    public String telefono;
    public String asunto = "Otro motivo";
    public String mensaje;
    public LocalDateTime fecha_envio;

    /** sin_leer | leido | respondido (ENUM de la tabla). */
    public String estado = "sin_leer";
}
