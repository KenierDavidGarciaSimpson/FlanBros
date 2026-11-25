package com.example.demo.Servicio;

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
 * <p>Si no hay credenciales SMTP configuradas ({@code MAIL_USUARIO}), no se intenta
 * enviar y se registra una advertencia; el endpoint responde igual ("si el correo
 * esta registrado, recibiras instrucciones") para no revelar que cuentas existen.</p>
 */
@Service
public class CorreoServicio {

    private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);

    private final JavaMailSender mailSender;
    private final String usuarioSmtp;
    private final String remitente;

    public CorreoServicio(JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String usuarioSmtp,
            @Value("${flanbross.recuperacion.correo-remitente:}") String remitente) {
        this.mailSender = mailSender;
        this.usuarioSmtp = usuarioSmtp;
        this.remitente = remitente;
    }

    /** Envia el token de recuperacion al correo indicado. */
    public void enviarTokenRecuperacion(String destino, String token) {
        if (usuarioSmtp == null || usuarioSmtp.isBlank()) {
            log.warn("Recuperacion: no hay SMTP configurado (MAIL_USUARIO). "
                    + "No se envio el token a {}", destino);
            return;
        }

        String desde = (remitente == null || remitente.isBlank()) ? usuarioSmtp : remitente;
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(desde);
            mensaje.setTo(destino);
            mensaje.setSubject("Recuperacion de contrasena - FlanBross");
            mensaje.setText("Hola,\n\n"
                    + "Recibimos una solicitud para restablecer tu contrasena.\n\n"
                    + "Tu token de recuperacion es: " + token + "\n\n"
                    + "El token vence en 1 hora y solo se puede usar una vez.\n"
                    + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                    + "FlanBross");
            mailSender.send(mensaje);
            log.info("Token de recuperacion enviado a {}", destino);
        } catch (Exception e) {
            // No se propaga: la respuesta al usuario debe ser generica.
            log.error("No se pudo enviar el correo de recuperacion a {}: {}", destino, e.getMessage());
        }
    }
}
