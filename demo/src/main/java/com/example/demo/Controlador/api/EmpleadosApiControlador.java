package com.example.demo.Controlador.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.Empleado;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.PersonaRepositorio;

/**
 * EmpleadosApiControlador — capa 3 (Controlador), API de empleados.
 *
 * <p>Sustituye a {@code backend/api/empleados.php} (listar, crear, editar y dar de
 * baja). Lo usa {@code admin-empleados.js}.</p>
 *
 * <p>Crear un empleado NO crea su cuenta de acceso: en el diseno original el alta de
 * credenciales se hace aparte. Se respeta tal cual para no inventar un flujo de
 * seguridad nuevo en esta fase.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/empleados")
public class EmpleadosApiControlador {

    private final PersonaRepositorio personas;

    public EmpleadosApiControlador(PersonaRepositorio personas) {
        this.personas = personas;
    }

    /** GET /api/empleados — solo los activos, con el nombre de su rol. */
    @GetMapping
    public RespuestaApi listar() {
        return RespuestaApi.ok(personas.listarEmpleadosActivos());
    }

    /** POST /api/empleados — alta de personal. */
    @PostMapping
    public ResponseEntity<RespuestaApi> crear(@RequestBody Map<String, Object> cuerpo) {
        String nombre = PeticionUtil.texto(cuerpo, "nombre");
        String apellido = PeticionUtil.texto(cuerpo, "apellido");
        String correo = PeticionUtil.texto(cuerpo, "correo");
        int idRol = PeticionUtil.entero(cuerpo, "id_rol");

        if (nombre == null || apellido == null || correo == null || idRol <= 0) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "Faltan datos obligatorios: nombre, apellido, correo e id_rol"));
        }
        if (personas.existeCorreo(correo)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Ese correo ya esta registrado"));
        }

        Empleado empleado = new Empleado();
        empleado.nombre = nombre;
        empleado.apellido = apellido;
        empleado.correo = correo;
        empleado.documento = PeticionUtil.texto(cuerpo, "documento");
        empleado.telefono = PeticionUtil.texto(cuerpo, "telefono");
        empleado.salario = PeticionUtil.decimal(cuerpo, "salario");
        empleado.id_rol = idRol;
        empleado.fecha_contratacion = LocalDate.now();
        empleado.estado_empleado = "activo";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.ok(personas.crearEmpleado(empleado), "Empleado creado"));
    }

    /** PUT /api/empleados — edicion de nombre, salario y estado. */
    @PutMapping
    public ResponseEntity<RespuestaApi> actualizar(@RequestBody Map<String, Object> cuerpo) {
        int idEmpleado = PeticionUtil.entero(cuerpo, "id_empleado");
        BigDecimal salario = PeticionUtil.decimal(cuerpo, "salario");
        String estado = PeticionUtil.texto(cuerpo, "estado_empleado", PeticionUtil.texto(cuerpo, "estado"));

        boolean actualizado = personas.actualizarEmpleado(idEmpleado,
                PeticionUtil.texto(cuerpo, "nombre"), salario, estado);
        if (!actualizado) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Empleado no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(
                personas.buscarEmpleado(idEmpleado).orElse(null), "Empleado actualizado"));
    }

    /** DELETE /api/empleados/{id} — baja logica. */
    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApi> desactivar(@PathVariable int id) {
        return desactivarPorId(id);
    }

    /** Alias con el id en la query o en el cuerpo ({@code {"id_empleado": N}}). */
    @DeleteMapping
    public ResponseEntity<RespuestaApi> desactivarPorParametro(
            @RequestParam(required = false) Integer id,
            @RequestBody(required = false) Map<String, Object> cuerpo) {

        int idEmpleado = id != null ? id : PeticionUtil.entero(cuerpo, "id_empleado");
        if (idEmpleado <= 0) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Falta el id del empleado"));
        }
        return desactivarPorId(idEmpleado);
    }

    private ResponseEntity<RespuestaApi> desactivarPorId(int id) {
        if (!personas.desactivarEmpleado(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Empleado no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(null, "Empleado desactivado"));
    }
}
