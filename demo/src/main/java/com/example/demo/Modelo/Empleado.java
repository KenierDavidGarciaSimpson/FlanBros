package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Empleado — capa 1 (Modelo).
 *
 * <p>Copia de la tabla {@code Empleado} de {@code flanbross.sql}: el personal de
 * FlanBross (administrador, repostero, contador y domiciliario).</p>
 *
 * <p>{@code nombre_rol} no es columna: es el dato que trae el JOIN de
 * {@code empleados.php} para la tabla del panel.</p>
 *
 * <p>Capa: Modelo.</p>
 */
public class Empleado {

    public int id_empleado;
    public String nombre;
    public String apellido;
    public String documento;
    public String telefono;
    public String correo;

    /** DECIMAL(12,2). */
    public BigDecimal salario;

    public LocalDate fecha_contratacion;

    /** activo | inactivo (ENUM de la tabla). */
    public String estado_empleado = "activo";

    public int id_rol;

    /** Calculado: nombre del rol (JOIN con Rol, como en empleados.php). */
    public String nombre_rol;
}
