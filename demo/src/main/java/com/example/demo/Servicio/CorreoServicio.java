package com.example.demo.Servicio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * CorreoServicio — envia los correos transaccionales de la aplicacion.
 *
 * <p>Se usa para la recuperacion de contrasena: el token generado por el backend
 * viaja UNICAMENTE al correo del usuario, nunca en la respuesta HTTP.</p>
 *
 * <p>Dos formas de enviar, en este orden:</p>
 * <ol>
 *   <li><b>API HTTP de Brevo</b> ({@code BREVO_API_KEY}). Es la que se usa en Render:
 *       el plan gratuito de Render bloquea los puertos SMTP (25, 465 y 587), pero no
 *       el 443 (HTTPS), que es por donde va esta API.</li>
 *   <li><b>SMTP</b> ({@code MAIL_USUARIO}, por ejemplo Gmail con contrasena de
 *       aplicacion). Sirve para correr el proyecto en un computador local.</li>
 * </ol>
 *
 * <p>Si no hay ninguna configurada, no se envia nada y se registra una advertencia;
 * el endpoint responde igual ("si el correo esta registrado, recibiras instrucciones")
 * para no revelar que cuentas existen.</p>
 */
@Service
public class CorreoServicio {

    private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final String usuarioSmtp;
    private final String remitente;
    private final String claveBrevo;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CorreoServicio(JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String usuarioSmtp,
            @Value("${flanbross.recuperacion.correo-remitente:}") String remitente,
            @Value("${flanbross.recuperacion.brevo-api-key:}") String claveBrevo) {
        this.mailSender = mailSender;
        this.usuarioSmtp = usuarioSmtp;
        this.remitente = remitente;
        this.claveBrevo = claveBrevo;
    }

    /** Envia el token de recuperacion al correo indicado. */
    public void enviarTokenRecuperacion(String destino, String token) {
        String asunto = "Recuperación de contraseña - FlanBross";
        String texto = "Hola,\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                + "Tu token de recuperación es: " + token + "\n\n"
                + "El token vence en 1 hora y solo se puede usar una vez.\n"
                + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                + "FlanBross";

        if (tieneTexto(claveBrevo)) {
            enviarConBrevo(destino, asunto, texto);
        } else if (tieneTexto(usuarioSmtp)) {
            enviarConSmtp(destino, asunto, texto);
        } else {
            log.warn("Recuperacion: no hay correo configurado (BREVO_API_KEY o MAIL_USUARIO). "
                    + "No se envio el token a {}", destino);
        }
    }

    private void enviarConBrevo(String destino, String asunto, String texto) {
        if (!tieneTexto(remitente)) {
            log.error("Recuperacion: falta MAIL_REMITENTE (correo verificado en Brevo). No se envio a {}", destino);
            return;
        }
        String cuerpo = "{"
                + "\"sender\":{\"name\":\"FlanBross\",\"email\":" + json(remitente) + "},"
                + "\"to\":[{\"email\":" + json(destino) + "}],"
                + "\"subject\":" + json(asunto) + ","
                + "\"textContent\":" + json(texto)
                + "}";
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_BREVO))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", claveBrevo)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 == 2) {
                log.info("Token de recuperacion enviado a {} (Brevo)", destino);
            } else {
                log.error("Brevo rechazo el correo a {}: HTTP {} {}", destino,
                        respuesta.statusCode(), respuesta.body());
            }
        } catch (Exception e) {
            // No se propaga: la respuesta al usuario debe ser generica.
            log.error("No se pudo enviar el correo de recuperacion a {} (Brevo): {}", destino, e.getMessage());
        }
    }

    private void enviarConSmtp(String destino, String asunto, String texto) {
        String desde = tieneTexto(remitente) ? remitente : usuarioSmtp;
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(desde);
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            mailSender.send(mensaje);
            log.info("Token de recuperacion enviado a {} (SMTP)", destino);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de recuperacion a {} (SMTP): {}", destino, e.getMessage());
        }
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }

    /** Convierte un texto en una cadena JSON con sus caracteres escapados. */
    private static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
