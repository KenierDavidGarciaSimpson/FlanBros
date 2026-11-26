package com.example.demo.Seguridad;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.demo.Modelo.dto.SesionUsuario;

public final class UsuarioActual {

    public static final String CLIENTE = "cliente";
    public static final String DOMICILIARIO = "domiciliario";

    private UsuarioActual() {
    }

    public static Optional<SesionUsuario> obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof SesionUsuario sesion) {
            return Optional.of(sesion);
        }
        return Optional.empty();
    }

    public static Optional<Integer> clientePropio() {
        return idSiTieneRol(CLIENTE);
    }

    public static Optional<Integer> domiciliarioPropio() {
        return idSiTieneRol(DOMICILIARIO);
    }

    private static Optional<Integer> idSiTieneRol(String rol) {
        return obtener()
                .filter(usuario -> rol.equalsIgnoreCase(usuario.nombre_rol))
                .map(usuario -> usuario.id_entidad);
    }
}
