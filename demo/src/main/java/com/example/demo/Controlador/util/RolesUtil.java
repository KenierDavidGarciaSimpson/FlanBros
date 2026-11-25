package com.example.demo.Controlador.util;

import java.util.List;
import java.util.Locale;

/**
 * RolesUtil — roles del sistema y comprobaciones de acceso.
 *
 * <p>CONVERTIDO (parcial) desde: {@code frontend/js/modules/sesion.js} y
 * {@code frontend/admin/js/admin-nav.js}</p>
 * <ul>
 *   <li>arreglo {@code rolesAdmin} de sesion.js -&gt; {@link #ROLES_ADMIN}</li>
 *   <li>comprobacion de {@code requiereAdmin()} -&gt; {@link #esRolAdmin(String)}</li>
 *   <li>rama {@code _u.nombre_rol === 'domiciliario'} de admin-nav.js -&gt; {@link #esDomiciliario(String)}</li>
 * </ul>
 *
 * <p>NO convertido de sesion.js (depende del navegador):</p>
 * <ul>
 *   <li>{@code obtenerSesion()} / {@code guardarSesion()} -&gt; usan {@code localStorage}.
 *       En Spring el equivalente es {@code HttpSession} o Spring Security
 *       (spring-boot-starter-security NO esta instalado; no se anadio ninguna dependencia).</li>
 *   <li>{@code cerrarSesion()} / {@code requiereSesion()} / {@code requiereAdmin()}
 *       -&gt; cambian {@code window.location.href}; en Java eso es redireccion
 *       ({@code redirect:}) o un filtro/interceptor, que es configuracion, no traduccion.</li>
 * </ul>
 *
 * <p>Capa: Controlador. No toca la base de datos.</p>
 */
public final class RolesUtil {

    public static final String ADMINISTRADOR = "administrador";
    public static final String REPOSTERO = "repostero";
    public static final String CONTADOR = "contador";
    public static final String DOMICILIARIO = "domiciliario";

    /** Mismo contenido que {@code const rolesAdmin} de sesion.js. */
    public static final List<String> ROLES_ADMIN =
            List.of(ADMINISTRADOR, REPOSTERO, CONTADOR, DOMICILIARIO);

    private RolesUtil() {
        // clase de utilidades: no se instancia
    }

    /** true si el rol puede entrar al panel de administracion. */
    public static boolean esRolAdmin(String nombreRol) {
        if (nombreRol == null) {
            return false;
        }
        return ROLES_ADMIN.contains(nombreRol.trim().toLowerCase(Locale.ROOT));
    }

    /** true si el rol es el del repartidor (en admin-nav.js se le manda a domiciliario.html). */
    public static boolean esDomiciliario(String nombreRol) {
        if (nombreRol == null) {
            return false;
        }
        return DOMICILIARIO.equals(nombreRol.trim().toLowerCase(Locale.ROOT));
    }
}
