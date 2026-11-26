package com.example.demo.Controlador.api;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Controlador.util.ValidacionUtil;
import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Modelo.dto.SesionUsuario;
import com.example.demo.Persistencia.PersonaRepositorio;
import com.example.demo.Seguridad.LimitadorIntentos;
import com.example.demo.Seguridad.UsuarioActual;
import com.example.demo.Servicio.CorreoServicio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class SesionApiControlador {

    private static final int LARGO_MINIMO_CONTRASENA = 8;

    private final PersonaRepositorio personas;
    private final LimitadorIntentos limitador;
    private final CorreoServicio correoServicio;
    private final SecurityContextRepository repositorioContexto = new HttpSessionSecurityContextRepository();

    private final boolean mostrarToken;

    public SesionApiControlador(PersonaRepositorio personas, LimitadorIntentos limitador,
            CorreoServicio correoServicio,
            @Value("${flanbross.recuperacion.mostrar-token:false}") boolean mostrarToken) {
        this.personas = personas;
        this.limitador = limitador;
        this.correoServicio = correoServicio;
        this.mostrarToken = mostrarToken;
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaApi> login(@RequestBody Map<String, Object> cuerpo,
            HttpServletRequest solicitud, HttpServletResponse respuesta) {
        String correo = PeticionUtil.texto(cuerpo, "correo");
        String contrasena = PeticionUtil.texto(cuerpo, "contrasena");

        if (correo == null || contrasena == null) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Ingresa tu correo y tu contrasena"));
        }

        String clave = solicitud.getRemoteAddr() + "|" + correo.toLowerCase(Locale.ROOT);
        if (limitador.bloqueado(clave)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(RespuestaApi.error(
                    "Demasiados intentos fallidos. Espera 15 minutos e intentalo de nuevo"));
        }
        Optional<SesionUsuario> sesion = personas.autenticar(correo, contrasena);
        if (sesion.isEmpty()) {
            limitador.registrarFallo(clave);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RespuestaApi.error("Correo o contrasena incorrectos"));
        }
        limitador.limpiar(clave);
        iniciarSesion(sesion.get(), solicitud, respuesta);
        return ResponseEntity.ok(RespuestaApi.ok(conUsuario(sesion.get()),
                "Bienvenido, " + sesion.get().nombre));
    }

    @GetMapping("/sesion")
    public ResponseEntity<RespuestaApi> sesionActual() {
        return UsuarioActual.obtener()
                .<ResponseEntity<RespuestaApi>>map(usuario -> ResponseEntity.ok(RespuestaApi.ok(conUsuario(usuario))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(RespuestaApi.error("No hay sesion iniciada")));
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaApi> registro(@RequestBody Map<String, Object> cuerpo,
            HttpServletRequest solicitud, HttpServletResponse respuesta) {
        String nombre = PeticionUtil.texto(cuerpo, "nombre");
        String apellido = PeticionUtil.texto(cuerpo, "apellido");
        String correo = PeticionUtil.texto(cuerpo, "correo");
        String contrasena = PeticionUtil.texto(cuerpo, "contrasena");

        String telefono = PeticionUtil.texto(cuerpo, "telefono");
        String direccion = PeticionUtil.texto(cuerpo, "direccion");

        if (nombre == null || apellido == null || correo == null || contrasena == null) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Faltan datos obligatorios para crear la cuenta"));
        }
        String error = ValidacionUtil.primero(
                ValidacionUtil.nombre("nombre", nombre),
                ValidacionUtil.nombre("apellido", apellido),
                ValidacionUtil.correo(correo),
                ValidacionUtil.telefono(telefono),
                ValidacionUtil.largoMaximo("dirección", direccion, 200),
                ValidacionUtil.contrasena(contrasena));
        if (error != null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(error));
        }
        if (personas.existeCorreo(correo)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Ese correo ya está registrado"));
        }

        Cliente cliente = new Cliente();
        cliente.nombre = nombre;
        cliente.apellido = apellido;
        cliente.correo = correo;
        cliente.telefono = telefono;
        cliente.direccion = direccion;
        personas.registrarCliente(cliente, contrasena);

        Optional<SesionUsuario> sesion = personas.autenticar(correo, contrasena);
        if (sesion.isEmpty()) {
            return ResponseEntity.ok(RespuestaApi.ok(null,
                    "Cuenta creada. Ya puedes iniciar sesion"));
        }
        iniciarSesion(sesion.get(), solicitud, respuesta);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.ok(conUsuario(sesion.get()), "Cuenta creada correctamente"));
    }

    @PostMapping("/recuperar")
    public ResponseEntity<RespuestaApi> recuperar(@RequestBody Map<String, Object> cuerpo) {
        String token = PeticionUtil.texto(cuerpo, "token");
        String nuevaContrasena = PeticionUtil.texto(cuerpo, "nueva_contrasena");

        if (token != null) {
            return canjearToken(token, nuevaContrasena);
        }
        return solicitarToken(PeticionUtil.texto(cuerpo, "correo"));
    }

    private ResponseEntity<RespuestaApi> canjearToken(String token, String nuevaContrasena) {
        String error = ValidacionUtil.contrasena(nuevaContrasena);
        if (error != null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(error));
        }
        if (!personas.cambiarContrasena(token, nuevaContrasena)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Token inválido o expirado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(null, "Contraseña actualizada correctamente"));
    }

    private ResponseEntity<RespuestaApi> solicitarToken(String correo) {
        if (correo == null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error("Ingresa tu correo"));
        }
        String error = ValidacionUtil.correo(correo);
        if (error != null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(error));
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        Optional<String> generado = personas.solicitarToken(correo);
        if (mostrarToken) {
            generado.ifPresent(token -> datos.put("token", token));
        } else {
            generado.ifPresent(token -> correoServicio.enviarTokenRecuperacion(correo, token));
        }

        return ResponseEntity.ok(RespuestaApi.ok(datos,
                "Si el correo está registrado, recibirás las instrucciones"));
    }

    private void iniciarSesion(SesionUsuario sesion, HttpServletRequest solicitud,
            HttpServletResponse respuesta) {
        if (solicitud.getSession(false) != null) {
            solicitud.changeSessionId();
        }
        Authentication autenticacion = UsernamePasswordAuthenticationToken.authenticated(sesion, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + sesion.nombre_rol.toUpperCase(Locale.ROOT))));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, solicitud, respuesta);
    }

    private Map<String, Object> conUsuario(SesionUsuario sesion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("usuario", sesion);
        return datos;
    }
}
