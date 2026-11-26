package com.example.demo.Controlador.util;

import java.math.BigDecimal;
import java.util.Map;

public final class PeticionUtil {

    private PeticionUtil() {
    }

    public static String texto(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
        if (valor == null) {
            return null;
        }
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() ? null : texto;
    }

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
