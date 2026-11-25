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

/**
 * FormatoUtil — utilidades de presentacion.
 *
 * <p>CONVERTIDO desde: {@code frontend/js/modules/utils.js}</p>
 * <ul>
 *   <li>{@code formatearPrecio(num)}   -&gt; {@link #formatearPrecio(double)}</li>
 *   <li>{@code formatearFecha(str)}    -&gt; {@link #formatearFecha(String)}</li>
 *   <li>{@code etiquetaEstado(estado)} -&gt; {@link #etiquetaEstado(String)}</li>
 *   <li>{@code slugificar(texto)}      -&gt; {@link #slugificar(String)}</li>
 * </ul>
 *
 * <p>NO convertido de utils.js: {@code mostrarToast(mensaje, tipo)}. Manipula el DOM
 * ({@code document.createElement}, {@code appendChild}, {@code setTimeout}); eso es
 * vista, no logica de servidor, y no tiene equivalente 1 a 1 en Java.</p>
 *
 * <p>Capa: Controlador (logica de presentacion). No toca la base de datos.</p>
 */
public final class FormatoUtil {

    /** Locale del frontend: toLocaleString('es-CO') / toLocaleDateString('es-CO'). */
    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");

    /** Equivalente a { day:'numeric', month:'short', year:'numeric' } en es-CO. */
    private static final DateTimeFormatter FORMATO_FECHA_CORTA =
            DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO);

    /** Formatos que puede devolver PostgreSQL / el frontend. */
    private static final DateTimeFormatter[] FORMATOS_ENTRADA = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /** Mapa de estados tomado tal cual de etiquetaEstado() en utils.js. */
    private static final Map<String, String> ETIQUETAS_ESTADO = new HashMap<>();

    static {
        ETIQUETAS_ESTADO.put("pendiente", "Pendiente");
        ETIQUETAS_ESTADO.put("en_preparacion", "En preparación");
        ETIQUETAS_ESTADO.put("listo", "Listo para enviar");
        ETIQUETAS_ESTADO.put("entregado", "Entregado");
        ETIQUETAS_ESTADO.put("cancelado", "Cancelado");
    }

    private FormatoUtil() {
        // clase de utilidades: no se instancia
    }

    /**
     * 42000 -&gt; "$42.000". Usa el mismo redondeo que {@code Math.round()} de JavaScript.
     */
    public static String formatearPrecio(double valor) {
        return formatearPrecio(Math.round(valor));
    }

    /**
     * Version para los DECIMAL(12,2) de la base de datos ({@code Producto.precio}, etc.).
     */
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

    /**
     * LocalDate -&gt; "19 may 2026". Si la fecha es null devuelve "—", igual que el JS.
     */
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

    /**
     * Equivalente a {@code new Date(fechaStr)} + toLocaleDateString('es-CO').
     * Acepta "2026-05-19", "2026-05-19 10:30:00" y "2026-05-19T10:30:00".
     * Si no se puede interpretar devuelve "—" (el JS devolvia "—" con valores vacios).
     */
    public static String formatearFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return "—";
        }
        String limpio = fechaTexto.trim();

        try {
            return formatearFecha(LocalDate.parse(limpio));
        } catch (DateTimeParseException ignorado) {
            // no era una fecha simple: se prueban los formatos con hora
        }

        for (DateTimeFormatter formato : FORMATOS_ENTRADA) {
            try {
                return formatearFecha(LocalDateTime.parse(limpio, formato));
            } catch (DateTimeParseException ignorado) {
                // se prueba el siguiente formato
            }
        }

        if (limpio.length() > 10) {
            try {
                return formatearFecha(LocalDate.parse(limpio.substring(0, 10)));
            } catch (DateTimeParseException ignorado) {
                // se rinde
            }
        }

        return "—";
    }

    /**
     * Estado del pedido a texto en español. Devuelve el mismo texto recibido si no
     * esta en el mapa (comportamiento {@code map[estado] || estado} del JS).
     */
    public static String etiquetaEstado(String estado) {
        if (estado == null) {
            return null;
        }
        return ETIQUETAS_ESTADO.getOrDefault(estado, estado);
    }

    /**
     * "Torta Tres Leches" -&gt; "torta-tres-leches". Misma secuencia que el JS:
     * minusculas, quitar acentos y "ñ", eliminar lo que no sea a-z/0-9/espacio/guion,
     * recortar y convertir espacios en guiones.
     */
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
