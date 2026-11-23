package com.example.demo.Seguridad;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.demo.Modelo.dto.SesionUsuario;

/**
 * UsuarioActual — quien hace la peticion, segun la sesion del SERVIDOR.
 *
 * <p>Los controladores usan esta clase en lugar de fiarse de {@code id_cliente} o
 * {@code id_domiciliario} que llegan en la peticion: esos datos los controla el navegador,
 * y por tanto cualquiera podria consultar o modificar informacion ajena cambiandolos.</p>
 *
 * <p>Capa: Seguridad.</p>
 */
public final class UsuarioActual {

    public static final String CLIENTE = "cliente";
    public static final String DOMICILIARIO = "domiciliario";

    private UsuarioActual() {
        // clase de utilidades: no se instancia
    }

    /** Usuario autenticado de la peticion en curso, si lo hay. */
    public static Optional<SesionUsuario> obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof SesionUsuario sesion) {
            return Optional.of(sesion);
        }
        return Optional.empty();
    }

    /** {@code id_cliente} propio, solo si quien llama es un cliente. */
    public static Optional<Integer> clientePropio() {
        return idSiTieneRol(CLIENTE);
    }

    /** {@code id_empleado} propio, solo si quien llama es un domiciliario. */
    public static Optional<Integer> domiciliarioPropio() {
        return idSiTieneRol(DOMICILIARIO);
    }

    private static Optional<Integer> idSiTieneRol(String rol) {
        return obtener()
                .filter(usuario -> rol.equalsIgnoreCase(usuario.nombre_rol))
                .map(usuario -> usuario.id_entidad);
    }
}
