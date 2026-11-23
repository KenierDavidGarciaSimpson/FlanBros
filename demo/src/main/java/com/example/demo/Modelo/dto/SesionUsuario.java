package com.example.demo.Modelo.dto;

/**
 * SesionUsuario — capa 1 (Modelo), objeto de transferencia.
 *
 * <p>Es exactamente el objeto {@code datos.usuario} que devolvia {@code login.php}
 * (y {@code registro.php} con el mismo formato) y que {@code sesion.js} guarda en
 * {@code localStorage} como {@code fb_usuario}:</p>
 *
 * <pre>{ "id_usuario": 7, "id_entidad": 3, "nombre": "Ana", "apellido": "Torres",
 *   "correo": "...", "nombre_rol": "cliente", "id_rol": 1 }</pre>
 *
 * <p>{@code id_entidad} es el id del cliente o del empleado segun el rol; es lo que
 * {@code checkout.js} manda como {@code id_cliente} al crear un pedido. No se
 * renombra a {@code id_cliente} porque romperia el frontend.</p>
 *
 * <p>Nunca lleva contrasena ni hash: es lo unico del modelo que sale al navegador
 * con datos personales.</p>
 *
 * <p>Capa: Modelo (DTO).</p>
 */
public class SesionUsuario implements java.io.Serializable {

    private static final long serialVersionUID = 1L;


    public int id_usuario;
    public int id_entidad;
    public String nombre;
    public String apellido;
    public String correo;
    public String nombre_rol;
    public int id_rol;

    /** true si el rol entra al panel de administracion. */
    public boolean esAdmin() {
        return "administrador".equals(nombre_rol)
                || "repostero".equals(nombre_rol)
                || "contador".equals(nombre_rol)
                || "domiciliario".equals(nombre_rol);
    }
}
