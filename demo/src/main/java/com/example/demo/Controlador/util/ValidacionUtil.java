package com.example.demo.Controlador.util;

import java.util.regex.Pattern;

/**
 * ValidacionUtil — capa 3 (Controlador), apoyo.
 *
 * <p>Reglas de formato que comparten la web, la app movil y la API. La interfaz
 * valida primero para dar respuesta rapida, pero la decision final es del servidor:
 * nadie puede saltarse estas reglas mandando la peticion a mano (Postman, curl).</p>
 *
 * <p>Cada metodo devuelve {@code null} si el dato es valido, o el mensaje de error
 * listo para mostrar al usuario.</p>
 */
public final class ValidacionUtil {

    /** Algo@algo.dominio, sin espacios. */
    private static final Pattern CORREO =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    /** Letras (con tildes y n), espacios, punto, apostrofo y guion. Empieza con letra. */
    private static final Pattern NOMBRE =
            Pattern.compile("^\\p{L}[\\p{L} .'-]*$");

    /** Opcional + y de 7 a 15 digitos (se ignoran espacios y guiones al validar). */
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9]{7,15}$");

    public static final int CONTRASENA_MIN = 8;
    /** BCrypt solo usa los primeros 72 bytes. */
    public static final int CONTRASENA_MAX = 72;

    private ValidacionUtil() {
        // Clase de utilidades: no se instancia.
    }

    public static String correo(String correo) {
        if (correo == null) {
            return "El correo es obligatorio";
        }
        if (correo.length() > 120 || !CORREO.matcher(correo).matches()) {
            return "Escribe un correo válido (ejemplo: nombre@correo.com)";
        }
        return null;
    }

    public static String nombre(String campo, String valor) {
        if (valor == null) {
            return "El campo " + campo + " es obligatorio";
        }
        if (valor.length() < 2 || valor.length() > 80) {
            return "El campo " + campo + " debe tener entre 2 y 80 caracteres";
        }
        if (!NOMBRE.matcher(valor).matches()) {
            return "El campo " + campo + " solo puede tener letras y espacios";
        }
        return null;
    }

    /** El telefono es opcional: null o vacio es valido. */
    public static String telefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String limpio = telefono.replaceAll("[\\s-]", "");
        if (!TELEFONO.matcher(limpio).matches()) {
            return "El teléfono debe tener entre 7 y 15 dígitos";
        }
        return null;
    }

    public static String contrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < CONTRASENA_MIN) {
            return "La contraseña debe tener al menos " + CONTRASENA_MIN + " caracteres";
        }
        if (contrasena.length() > CONTRASENA_MAX) {
            return "La contraseña no puede tener más de " + CONTRASENA_MAX + " caracteres";
        }
        return null;
    }

    /** Texto opcional con largo maximo. */
    public static String largoMaximo(String campo, String valor, int maximo) {
        if (valor != null && valor.length() > maximo) {
            return "El campo " + campo + " no puede tener más de " + maximo + " caracteres";
        }
        return null;
    }

    /** Texto obligatorio con largo minimo y maximo. */
    public static String largoEntre(String campo, String valor, int minimo, int maximo) {
        if (valor == null || valor.length() < minimo) {
            return "El campo " + campo + " debe tener al menos " + minimo + " caracteres";
        }
        return largoMaximo(campo, valor, maximo);
    }

    /** Devuelve el primer error no nulo, o null si todos pasaron. */
    public static String primero(String... errores) {
        for (String error : errores) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }
}
