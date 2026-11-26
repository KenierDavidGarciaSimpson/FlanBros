package com.example.demo.Controlador.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PermisosPanelUtil {

    public static final List<String> SECCIONES = List.of(
            "dashboard", "pedidos", "productos", "inventario", "clientes",
            "pagos", "facturas", "domicilios", "empleados");

    private static final Map<String, List<String>> PERMISOS = Map.of(
            RolesUtil.ADMINISTRADOR, SECCIONES,
            RolesUtil.REPOSTERO, List.of("productos", "inventario"),
            RolesUtil.CONTADOR, List.of("facturas", "pagos"));

    private static final Map<String, String> TITULOS = Map.of(
            "dashboard", "Dashboard",
            "pedidos", "Pedidos",
            "productos", "Productos",
            "inventario", "Inventario",
            "clientes", "Clientes",
            "pagos", "Pagos",
            "facturas", "Facturas",
            "domicilios", "Domicilios",
            "empleados", "Empleados");

    private PermisosPanelUtil() {
    }

    public static List<String> seccionesPermitidas(String nombreRol) {
        if (nombreRol == null) {
            return List.of();
        }
        List<String> permitidas = PERMISOS.get(nombreRol.trim().toLowerCase(Locale.ROOT));
        return permitidas == null ? List.of() : permitidas;
    }

    public static String seccionInicial(String nombreRol) {
        List<String> permitidas = seccionesPermitidas(nombreRol);
        return permitidas.isEmpty() ? "dashboard" : permitidas.get(0);
    }

    public static boolean puedeVer(String nombreRol, String seccion) {
        return seccionesPermitidas(nombreRol).contains(seccion);
    }

    public static String titulo(String seccion) {
        if (seccion == null) {
            return null;
        }
        return TITULOS.getOrDefault(seccion, seccion);
    }
}
