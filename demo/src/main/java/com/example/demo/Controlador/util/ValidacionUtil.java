package com.example.demo.Controlador.util;

import java.util.regex.Pattern;

public final class ValidacionUtil {

    private static final Pattern CORREO =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private static final Pattern NOMBRE =
            Pattern.compile("^\\p{L}[\\p{L} .'-]*$");

    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9]{7,15}$");

    public static final int CONTRASENA_MIN = 8;
    public static final int CONTRASENA_MAX = 72;

    private ValidacionUtil() {
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

    public static String largoMaximo(String campo, String valor, int maximo) {
        if (valor != null && valor.length() > maximo) {
            return "El campo " + campo + " no puede tener más de " + maximo + " caracteres";
        }
        return null;
    }

    public static String largoEntre(String campo, String valor, int minimo, int maximo) {
        if (valor == null || valor.length() < minimo) {
            return "El campo " + campo + " debe tener al menos " + minimo + " caracteres";
        }
        return largoMaximo(campo, valor, maximo);
    }

    public static String primero(String... errores) {
        for (String error : errores) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }
}
