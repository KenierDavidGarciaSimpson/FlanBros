package com.example.demo.Modelo.dto;

public class SesionUsuario implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public int id_usuario;
    public int id_entidad;
    public String nombre;
    public String apellido;
    public String correo;
    public String nombre_rol;
    public int id_rol;

    public boolean esAdmin() {
        return "administrador".equals(nombre_rol)
                || "repostero".equals(nombre_rol)
                || "contador".equals(nombre_rol)
                || "domiciliario".equals(nombre_rol);
    }
}
