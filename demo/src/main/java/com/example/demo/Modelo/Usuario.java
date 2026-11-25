package com.example.demo.Modelo;

import java.time.LocalDateTime;

/**
 * Usuario — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Usuario} de {@code flanbross.sql}: las credenciales.
 * Un usuario es de un cliente O de un empleado (uno de los dos id va en NULL),
 * nunca de los dos, y siempre tiene un rol.</p>
 *
 * <p>El correo no vive en esta tabla en la base (esta en {@code Cliente} o en
 * {@code Empleado}), pero se copia aqui para poder buscar por correo sin hacer dos
 * JOIN. Es una desnormalizacion consciente y documentada, no un olvido.</p>
 *
 * <p><b>Contrasenas:</b> en la base el campo es {@code contrasena_hash} (bcrypt).
 * Sin base de datos no hay bcrypt, asi que el archivo de datos de la demo guarda la
 * contrasena en {@code contrasena_demo} y {@code contrasena_hash} queda null. El
 * dia que se conecte PostgreSQL solo hay que cambiar la implementacion del repositorio;
 * ni el modelo ni el controlador se tocan.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Usuario {

    public int id_usuario;
    public int id_rol;

    /** id del cliente, o null si el usuario es de un empleado. */
    public Integer id_cliente;

    /** id del empleado, o null si el usuario es de un cliente. */
    public Integer id_empleado;

    /** Copia del correo de Cliente/Empleado, para buscar sin JOIN. */
    public String correo;

    /** Hash bcrypt. Null mientras se trabaje sin base de datos. */
    public String contrasena_hash;

    /** SOLO DEMO (sin base de datos). Nunca se devuelve al navegador. */
    public String contrasena_demo;

    /** Token de recuperacion de contrasena y su vencimiento (recuperar.php). */
    public String token_reset;
    public LocalDateTime token_expira;

    /** true si es una cuenta de cliente (no de empleado). */
    public boolean esDeCliente() {
        return id_cliente != null;
    }
}
