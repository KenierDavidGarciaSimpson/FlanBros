package com.example.demo.Controlador.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FormatoUtil {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");

    private static final DateTimeFormatter FORMATO_FECHA_CORTA =
            DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO);

    private static final DateTimeFormatter[] FORMATOS_ENTRADA = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private static final Map<String, String> ETIQUETAS_ESTADO = new HashMap<>();

    static {
        ETIQUETAS_ESTADO.put("pendiente", "Pendiente");
        ETIQUETAS_ESTADO.put("en_preparacion", "En preparación");
        ETIQUETAS_ESTADO.put("listo", "Listo para enviar");
        ETIQUETAS_ESTADO.put("entregado", "Entregado");
        ETIQUETAS_ESTADO.put("cancelado", "Cancelado");
    }

    private FormatoUtil() {
    }

    public static String formatearPrecio(double valor) {
        return formatearPrecio(Math.round(valor));
    }

    public static String formatearPrecio(BigDecimal valor) {
        if (valor == null) {
            return formatearPrecio(0L);
        }
        return formatearPrecio(valor.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    public static String formatearPrecio(long valor) {
        NumberFormat formato = NumberFormat.getIntegerInstance(ES_CO);
        formato.setGroupingUsed(true);
        return "$" + formato.format(valor);
    }

    public static String formatearFecha(LocalDate fecha) {
        if (fecha == null) {
            return "—";
        }
        return FORMATO_FECHA_CORTA.format(fecha);
    }

    public static String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) {
            return "—";
        }
        return formatearFecha(fecha.toLocalDate());
    }

    public static String formatearFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return "—";
        }
        String limpio = fechaTexto.trim();

        try {
            return formatearFecha(LocalDate.parse(limpio));
        } catch (DateTimeParseException ignorado) {
        }

        for (DateTimeFormatter formato : FORMATOS_ENTRADA) {
            try {
                return formatearFecha(LocalDateTime.parse(limpio, formato));
            } catch (DateTimeParseException ignorado) {
            }
        }

        if (limpio.length() > 10) {
            try {
                return formatearFecha(LocalDate.parse(limpio.substring(0, 10)));
            } catch (DateTimeParseException ignorado) {
            }
        }

        return "—";
    }

    public static String etiquetaEstado(String estado) {
        if (estado == null) {
            return null;
        }
        return ETIQUETAS_ESTADO.getOrDefault(estado, estado);
    }

    public static String slugificar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
