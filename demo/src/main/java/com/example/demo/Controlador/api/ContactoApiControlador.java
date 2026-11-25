package com.example.demo.Controlador.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.MensajeContacto;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.ContactoRepositorio;

/**
 * ContactoApiControlador — capa 3 (Controlador), API del formulario de contacto.
 *
 * <p>Sustituye a {@code backend/api/contacto.php}, que era el unico endpoint que no
 * tocaba la base de datos de negocio. {@code contacto.html} manda
 * {@code {nombre, correo, telefono, asunto, mensaje}} y solo mira {@code exito}.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/contacto")
public class ContactoApiControlador {

    private final ContactoRepositorio mensajes;

    public ContactoApiControlador(ContactoRepositorio mensajes) {
        this.mensajes = mensajes;
    }

    /** POST /api/contacto — guarda el mensaje del visitante. */
    @PostMapping
    public ResponseEntity<RespuestaApi> enviar(@RequestBody Map<String, Object> cuerpo) {
        String nombre = PeticionUtil.texto(cuerpo, "nombre");
        String correo = PeticionUtil.texto(cuerpo, "correo");
        String mensaje = PeticionUtil.texto(cuerpo, "mensaje");

        if (nombre == null || correo == null || mensaje == null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "Por favor completa tu nombre, correo y mensaje"));
        }

        MensajeContacto contacto = new MensajeContacto();
        contacto.nombre = nombre;
        contacto.correo = correo;
        contacto.telefono = PeticionUtil.texto(cuerpo, "telefono");
        contacto.asunto = PeticionUtil.texto(cuerpo, "asunto", "Otro motivo");
        contacto.mensaje = mensaje;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.ok(mensajes.guardar(contacto),
                        "Mensaje enviado. Te responderemos pronto"));
    }

    /** GET /api/contacto — bandeja de mensajes para el panel. */
    @GetMapping
    public RespuestaApi listar() {
        return RespuestaApi.ok(mensajes.listar());
    }
}
