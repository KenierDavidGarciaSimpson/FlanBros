package com.example.demo.Controlador.api;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.MensajeContacto;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Modelo.dto.SesionUsuario;
import com.example.demo.Persistencia.ContactoRepositorio;
import com.example.demo.Persistencia.PersonaRepositorio;
import com.example.demo.Seguridad.UsuarioActual;

/**
 * ContactoApiControlador — capa 3 (Controlador), API del formulario de contacto.
 *
 * <p>El mensaje solo puede enviarse con sesion iniciada. El nombre, el correo y el
 * telefono se toman de la cuenta del usuario (nunca del formulario); por eso
 * {@code contacto.html} solo envia {@code {mensaje}}.</p>
 *
 * <p>El mensaje queda en la bandeja del panel de administracion
 * ({@code mensaje_contacto}).</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/contacto")
public class ContactoApiControlador {

    private final ContactoRepositorio mensajes;
    private final PersonaRepositorio personas;

    public ContactoApiControlador(ContactoRepositorio mensajes, PersonaRepositorio personas) {
        this.mensajes = mensajes;
        this.personas = personas;
    }

    /** POST /api/contacto — guarda el mensaje del usuario autenticado. */
    @PostMapping
    public ResponseEntity<RespuestaApi> enviar(@RequestBody Map<String, Object> cuerpo) {
        Optional<SesionUsuario> sesion = UsuarioActual.obtener();
        if (sesion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RespuestaApi.error("Inicia sesion para enviar un mensaje"));
        }

        String mensaje = PeticionUtil.texto(cuerpo, "mensaje");
        if (mensaje == null || mensaje.isBlank()) {
            return ResponseEntity.badRequest().body(RespuestaApi.error("Escribe tu mensaje"));
        }

        SesionUsuario usuario = sesion.get();
        String nombre = (usuario.nombre + " "
                + (usuario.apellido == null ? "" : usuario.apellido)).trim();
        // El telefono no viaja en la sesion: se toma de la ficha del cliente o empleado.
        String telefono = "cliente".equals(usuario.nombre_rol)
                ? personas.buscarCliente(usuario.id_entidad).map(c -> c.telefono).orElse(null)
                : personas.buscarEmpleado(usuario.id_entidad).map(e -> e.telefono).orElse(null);

        MensajeContacto contacto = new MensajeContacto();
        contacto.nombre = nombre;
        contacto.correo = usuario.correo;
        contacto.telefono = telefono;
        contacto.asunto = "Mensaje desde la web";
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

    /** PUT /api/contacto — marca un mensaje como leido (solo administrador). */
    @PutMapping
    public ResponseEntity<RespuestaApi> marcarLeido(@RequestBody Map<String, Object> cuerpo) {
        int id = PeticionUtil.entero(cuerpo, "id_mensaje");
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Falta el id del mensaje"));
        }
        if (!mensajes.marcarLeido(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Mensaje no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(null, "Mensaje marcado como leido"));
    }
}
