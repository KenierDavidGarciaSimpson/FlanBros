package com.example.demo.Modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Empleado {

    public int id_empleado;
    public String nombre;
    public String apellido;
    public String documento;
    public String telefono;
    public String correo;

    public BigDecimal salario;

    public LocalDate fecha_contratacion;

    public String estado_empleado = "activo";

    public int id_rol;

    public String nombre_rol;
}
