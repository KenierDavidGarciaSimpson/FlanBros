package com.example.demo.Controlador.util;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PeticionUtil — capa 3 (Controlador), apoyo.
 *
 * <p>Los endpoints reciben el cuerpo JSON como {@code Map<String,Object>} (igual que
 * {@code leerJSON()} en {@code helpers.php} devolvia un arreglo asociativo) y esta
 * clase lo lee sin reventar cuando falta una clave o llega un tipo raro.</p>
 *
 * <p>Regla: si el dato falta, devuelve {@code null} (los textos) o {@code 0} (los
 * enteros). Nunca lanza: un formulario incompleto debe responder "faltan datos",
 * no un 500.</p>
 *
 * <p>Capa: Controlador (apoyo). No conoce ni el Modelo ni la Persistencia.</p>
 */
public final class PeticionUtil {

    private PeticionUtil() {
        // Clase de utilidades: no se instancia.
    }

    /** Texto recortado, o null si la clave no viene. */
    public static String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
        if (valor == null) {
            return null;
        }
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() ? null : texto;
    }

    /** Texto recortado, o el valor por defecto si no viene o viene vacio. */
    public static String texto(Map<String, Object> cuerpo, String clave, String porDefecto) {
        String valor = texto(cuerpo, clave);
        return valor == null ? porDefecto : valor;
    }

    public static int entero(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        if (valor == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(valor).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Entero que puede venir vacio (por ejemplo id_domiciliario sin asignar). */
    public static Integer enteroOpcional(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
        if (valor == null || String.valueOf(valor).isBlank()) {
            return null;
        }
        int numero = entero(cuerpo, clave);
        return numero > 0 ? numero : null;
    }

    public static BigDecimal decimal(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(valor).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
