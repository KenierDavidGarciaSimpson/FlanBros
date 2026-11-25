package com.example.demo.Controlador.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PermisosPanelUtil — que secciones del panel de administracion ve cada rol.
 *
 * <p>CONVERTIDO desde: {@code frontend/admin/js/admin-nav.js}</p>
 * <ul>
 *   <li>objeto {@code _permisos} -&gt; {@link #PERMISOS}</li>
 *   <li>arreglo {@code secciones} -&gt; {@link #SECCIONES}</li>
 *   <li>objeto {@code titulos} -&gt; {@link #TITULOS}</li>
 *   <li>filtro de botones por rol -&gt; {@link #puedeVer(String, String)}</li>
 * </ul>
 *
 * <p>NO convertido de admin-nav.js: {@code mostrarSeccion(nombre)} y el ocultado de
 * botones ({@code btn.style.display}, {@code classList.toggle}, {@code getElementById}).
 * Eso es DOM: en Spring lo resuelve Thymeleaf sobre la vista, no una clase Java.</p>
 *
 * <p>Capa: Controlador. No toca la base de datos.</p>
 */
public final class PermisosPanelUtil {

    /** Mismo orden que el arreglo {@code secciones} de admin-nav.js. */
    public static final List<String> SECCIONES = List.of(
            "dashboard", "pedidos", "productos", "inventario", "clientes",
            "pagos", "facturas", "domicilios", "empleados");

    /** Copia literal del objeto {@code _permisos} de admin-nav.js. */
    private static final Map<String, List<String>> PERMISOS = Map.of(
            RolesUtil.ADMINISTRADOR, SECCIONES,
            RolesUtil.REPOSTERO, List.of("productos", "inventario"),
            RolesUtil.CONTADOR, List.of("facturas", "pagos"));

    /** Copia literal del objeto {@code titulos} de admin-nav.js. */
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
        // clase de utilidades: no se instancia
    }

    /**
     * Secciones visibles para el rol. Devuelve lista vacia si el rol no esta en el mapa
     * (mismo comportamiento que {@code _permisos[rol] || []} en el JS).
     */
    public static List<String> seccionesPermitidas(String nombreRol) {
        if (nombreRol == null) {
            return List.of();
        }
        List<String> permitidas = PERMISOS.get(nombreRol.trim().toLowerCase(Locale.ROOT));
        return permitidas == null ? List.of() : permitidas;
    }

    /** Primera seccion que debe abrirse al entrar (antes: {@code _permitidas[0] || 'dashboard'}). */
    public static String seccionInicial(String nombreRol) {
        List<String> permitidas = seccionesPermitidas(nombreRol);
        return permitidas.isEmpty() ? "dashboard" : permitidas.get(0);
    }

    public static boolean puedeVer(String nombreRol, String seccion) {
        return seccionesPermitidas(nombreRol).contains(seccion);
    }

    /** Titulo de la seccion; si no esta en el mapa devuelve el mismo nombre. */
    public static String titulo(String seccion) {
        if (seccion == null) {
            return null;
        }
        return TITULOS.getOrDefault(seccion, seccion);
    }
}
