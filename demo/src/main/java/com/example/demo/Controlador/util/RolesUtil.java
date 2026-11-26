package com.example.demo.Controlador.util;

import java.util.List;
import java.util.Locale;

public final class RolesUtil {

    public static final String ADMINISTRADOR = "administrador";
    public static final String REPOSTERO = "repostero";
    public static final String CONTADOR = "contador";
    public static final String DOMICILIARIO = "domiciliario";

    public static final List<String> ROLES_ADMIN =
            List.of(ADMINISTRADOR, REPOSTERO, CONTADOR, DOMICILIARIO);

    private RolesUtil() {
    }

    public static boolean esRolAdmin(String nombreRol) {
        if (nombreRol == null) {
            return false;
        }
        return ROLES_ADMIN.contains(nombreRol.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean esDomiciliario(String nombreRol) {
        if (nombreRol == null) {
            return false;
        }
        return DOMICILIARIO.equals(nombreRol.trim().toLowerCase(Locale.ROOT));
    }
}
