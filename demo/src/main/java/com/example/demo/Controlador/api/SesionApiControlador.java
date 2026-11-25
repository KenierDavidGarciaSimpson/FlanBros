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
import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Modelo.dto.SesionUsuario;
import com.example.demo.Persistencia.PersonaRepositorio;
import com.example.demo.Seguridad.LimitadorIntentos;
import com.example.demo.Seguridad.UsuarioActual;
import com.example.demo.Servicio.CorreoServicio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SesionApiControlador — capa 3 (Controlador), API de cuentas.
 *
 * <p>Une los tres endpoints de identidad del PHP original: {@code login.php},
 * {@code registro.php} y {@code recuperar.php}. Comparten un contrato que el
 * frontend da por hecho: <b>la sesion viaja siempre como {@code datos.usuario}</b>,
 * porque {@code login.html} y {@code registro.html} hacen
 * {@code guardarSesion(resp.datos.usuario)} sin comprobar nada mas.</p>
 *
 * <p>Ninguna respuesta incluye la contrasena: solo {@link SesionUsuario}.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api")
public class SesionApiControlador {

    private static final int LARGO_MINIMO_CONTRASENA = 8;

    private final PersonaRepositorio personas;
    private final LimitadorIntentos limitador;
    private final CorreoServicio correoServicio;
    private final SecurityContextRepository repositorioContexto = new HttpSessionSecurityContextRepository();

    /**
     * Solo para pruebas locales: si es true, el token se devuelve en la respuesta.
     * En produccion debe ser false para que el token viaje unicamente por correo.
     */
    private final boolean mostrarToken;

    public SesionApiControlador(PersonaRepositorio personas, LimitadorIntentos limitador,
            CorreoServicio correoServicio,
            @Value("${flanbross.recuperacion.mostrar-token:false}") boolean mostrarToken) {
        this.personas = personas;
        this.limitador = limitador;
        this.correoServicio = correoServicio;
        this.mostrarToken = mostrarToken;
    }

    /** POST /api/login — {correo, contrasena} -> {exito, mensaje, datos:{usuario}}. */
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

    /** POST /api/registro — alta de cliente; el rol lo fija el backend, no el formulario. */
    /**
     * Quien soy segun la sesion del SERVIDOR. La app movil lo llama al abrirse para saber si su
     * cookie de sesion sigue vigente (200 con el usuario, o 401). Tambien sirve para que el servidor
     * entregue la cookie XSRF-TOKEN antes del primer POST.
     */
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

        if (nombre == null || apellido == null || correo == null || contrasena == null) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Faltan datos obligatorios para crear la cuenta"));
        }
        if (contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "La contrasena debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres"));
        }
        if (personas.existeCorreo(correo)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Ese correo ya esta registrado"));
        }

        Cliente cliente = new Cliente();
        cliente.nombre = nombre;
        cliente.apellido = apellido;
        cliente.correo = correo;
        cliente.telefono = PeticionUtil.texto(cuerpo, "telefono");
        cliente.direccion = PeticionUtil.texto(cuerpo, "direccion");
        personas.registrarCliente(cliente, contrasena);

        // Se devuelve la sesion ya autenticada: registro.html entra directo a la cuenta.
        Optional<SesionUsuario> sesion = personas.autenticar(correo, contrasena);
        if (sesion.isEmpty()) {
            return ResponseEntity.ok(RespuestaApi.ok(null,
                    "Cuenta creada. Ya puedes iniciar sesion"));
        }
        iniciarSesion(sesion.get(), solicitud, respuesta);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.ok(conUsuario(sesion.get()), "Cuenta creada correctamente"));
    }

    /**
     * POST /api/recuperar — dos pasos en el mismo endpoint, como el PHP original:
     * con {@code correo} genera el token; con {@code token + nueva_contrasena} lo canjea.
     */
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
        if (nuevaContrasena == null || nuevaContrasena.length() < LARGO_MINIMO_CONTRASENA) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "La contrasena debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres"));
        }
        if (!personas.cambiarContrasena(token, nuevaContrasena)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Token invalido o expirado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(null, "Contrasena actualizada correctamente"));
    }

    private ResponseEntity<RespuestaApi> solicitarToken(String correo) {
        if (correo == null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error("Ingresa tu correo"));
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        // Contestamos igual exista o no el correo: no se revela quien esta registrado.
        Optional<String> generado = personas.solicitarToken(correo);
        if (mostrarToken) {
            // Solo para pruebas locales: el token se devuelve en la respuesta.
            generado.ifPresent(token -> datos.put("token", token));
        } else {
            // Flujo seguro: el token viaja solo al correo del usuario.
            generado.ifPresent(token -> correoServicio.enviarTokenRecuperacion(correo, token));
        }

        return ResponseEntity.ok(RespuestaApi.ok(datos,
                "Si el correo esta registrado, recibiras las instrucciones"));
    }

    /**
     * Crea la sesion HTTP del servidor. Se renueva el id de sesion (contra fijacion de sesion)
     * y el rol queda como autoridad {@code ROLE_<NOMBRE>}, que es lo que evaluan las rutas.
     */
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
