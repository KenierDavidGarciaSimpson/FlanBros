package com.example.demo.Persistencia.memoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.Empleado;
import com.example.demo.Modelo.Rol;
import com.example.demo.Modelo.Usuario;
import com.example.demo.Modelo.dto.SesionUsuario;
import com.example.demo.Persistencia.PersonaRepositorio;

@Profile("memoria")
@Repository
public class PersonaRepositorioMemoria implements PersonaRepositorio {

    private static final int ROL_CLIENTE = 1;

    private final List<Rol> roles;
    private final List<Cliente> clientes;
    private final List<Empleado> empleados;
    private final List<Usuario> usuarios;

    private int siguienteClienteId;
    private int siguienteEmpleadoId;
    private int siguienteUsuarioId;

    public PersonaRepositorioMemoria(LectorJson lector) {
        this.roles = lector.leerLista("datos/roles.json", Rol.class);
        this.clientes = new ArrayList<>(lector.leerLista("datos/clientes.json", Cliente.class));
        this.empleados = new ArrayList<>(lector.leerLista("datos/empleados.json", Empleado.class));
        this.usuarios = new ArrayList<>(lector.leerLista("datos/usuarios.json", Usuario.class));

        siguienteClienteId = clientes.stream()
                .mapToInt(cliente -> cliente.id_cliente).max().orElse(0) + 1;
        siguienteEmpleadoId = empleados.stream()
                .mapToInt(empleado -> empleado.id_empleado).max().orElse(0) + 1;
        siguienteUsuarioId = usuarios.stream()
                .mapToInt(usuario -> usuario.id_usuario).max().orElse(0) + 1;

        empleados.forEach(this::completarRol);
    }

    @Override
    public synchronized Optional<SesionUsuario> autenticar(String correo, String contrasena) {
        if (correo == null || contrasena == null) {
            return Optional.empty();
        }
        String buscado = correo.trim().toLowerCase(Locale.ROOT);
        return usuarios.stream()
                .filter(usuario -> usuario.correo != null
                        && usuario.correo.trim().toLowerCase(Locale.ROOT).equals(buscado))
                .filter(usuario -> contrasenaIgual(usuario, contrasena))
                .findFirst()
                .map(this::construirSesion);
    }

    private boolean contrasenaIgual(Usuario usuario, String contrasena) {
        return usuario.contrasena_demo != null && usuario.contrasena_demo.equals(contrasena);
    }

    private SesionUsuario construirSesion(Usuario usuario) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.id_usuario = usuario.id_usuario;
        sesion.id_rol = usuario.id_rol;
        sesion.correo = usuario.correo;
        sesion.nombre_rol = nombreRol(usuario.id_rol);

        if (usuario.esDeCliente()) {
            sesion.id_entidad = usuario.id_cliente;
            clientes.stream()
                    .filter(cliente -> cliente.id_cliente == usuario.id_cliente)
                    .findFirst()
                    .ifPresent(cliente -> {
                        sesion.nombre = cliente.nombre;
                        sesion.apellido = cliente.apellido;
                        sesion.correo = cliente.correo;
                    });
        } else if (usuario.id_empleado != null) {
            sesion.id_entidad = usuario.id_empleado;
            empleados.stream()
                    .filter(empleado -> empleado.id_empleado == usuario.id_empleado)
                    .findFirst()
                    .ifPresent(empleado -> {
                        sesion.nombre = empleado.nombre;
                        sesion.apellido = empleado.apellido;
                        sesion.correo = empleado.correo;
                    });
        }
        return sesion;
    }

    @Override
    public synchronized boolean existeCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        String buscado = correo.trim().toLowerCase(Locale.ROOT);
        return usuarios.stream().anyMatch(usuario -> mismoCorreo(usuario.correo, buscado))
                || clientes.stream().anyMatch(cliente -> mismoCorreo(cliente.correo, buscado))
                || empleados.stream().anyMatch(empleado -> mismoCorreo(empleado.correo, buscado));
    }

    @Override
    public synchronized Cliente registrarCliente(Cliente cliente, String contrasena) {
        cliente.id_cliente = siguienteClienteId++;
        cliente.fecha_registro = LocalDateTime.now();
        cliente.estado_cliente = "activo";
        clientes.add(cliente);

        Usuario cuenta = new Usuario();
        cuenta.id_usuario = siguienteUsuarioId++;
        cuenta.id_rol = ROL_CLIENTE;
        cuenta.id_cliente = cliente.id_cliente;
        cuenta.correo = cliente.correo;
        cuenta.contrasena_demo = contrasena;
        usuarios.add(cuenta);

        return cliente;
    }

    @Override
    public synchronized Optional<String> solicitarToken(String correo) {
        if (correo == null || correo.isBlank()) {
            return Optional.empty();
        }
        String buscado = correo.trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> cuenta = usuarios.stream()
                .filter(usuario -> mismoCorreo(usuario.correo, buscado))
                .findFirst();
        if (cuenta.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = cuenta.get();
        usuario.token_reset = UUID.randomUUID().toString().replace("-", "");
        usuario.token_expira = LocalDateTime.now().plusHours(1);
        return Optional.of(usuario.token_reset);
    }

    @Override
    public synchronized boolean cambiarContrasena(String token, String nuevaContrasena) {
        if (token == null || token.isBlank() || nuevaContrasena == null || nuevaContrasena.isBlank()) {
            return false;
        }
        String buscado = token.trim();
        for (Usuario usuario : usuarios) {
            if (buscado.equals(usuario.token_reset)) {
                boolean vigente = usuario.token_expira == null
                        || usuario.token_expira.isAfter(LocalDateTime.now());
                if (!vigente) {
                    return false;
                }
                usuario.contrasena_demo = nuevaContrasena;
                usuario.token_reset = null;
                usuario.token_expira = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized List<Cliente> listarClientes() {
        return clientes.stream()
                .sorted(Comparator.comparing(
                        (Cliente cliente) -> cliente.fecha_registro == null
                                ? LocalDateTime.MIN : cliente.fecha_registro).reversed())
                .toList();
    }

    @Override
    public synchronized Optional<Cliente> buscarCliente(int idCliente) {
        return clientes.stream()
                .filter(cliente -> cliente.id_cliente == idCliente)
                .findFirst();
    }

    @Override
    public synchronized boolean cambiarEstadoCliente(int idCliente, String estado) {
        if (estado == null || estado.isBlank()) {
            return false;
        }
        return buscarCliente(idCliente)
                .map(cliente -> {
                    cliente.estado_cliente = estado;
                    return true;
                })
                .orElse(false);
    }

    @Override
    public synchronized List<Empleado> listarEmpleadosActivos() {
        return empleados.stream()
                .filter(empleado -> "activo".equals(empleado.estado_empleado))
                .sorted(Comparator.comparing(empleado -> empleado.nombre == null ? "" : empleado.nombre,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public synchronized Empleado crearEmpleado(Empleado empleado, String contrasena) {
        empleado.id_empleado = siguienteEmpleadoId++;
        if (empleado.estado_empleado == null || empleado.estado_empleado.isBlank()) {
            empleado.estado_empleado = "activo";
        }
        empleados.add(empleado);
        completarRol(empleado);

        Usuario cuenta = new Usuario();
        cuenta.id_usuario = siguienteUsuarioId++;
        cuenta.id_rol = empleado.id_rol;
        cuenta.id_empleado = empleado.id_empleado;
        cuenta.correo = empleado.correo;
        cuenta.contrasena_demo = contrasena;
        usuarios.add(cuenta);
        return empleado;
    }

    @Override
    public synchronized Optional<Empleado> convertirClienteEnEmpleado(int idCliente, int idRol,
                                                                     BigDecimal salario, String documento) {
        Optional<Cliente> encontrado = buscarCliente(idCliente);
        Optional<Usuario> cuenta = usuarios.stream()
                .filter(usuario -> usuario.id_cliente != null && usuario.id_cliente == idCliente)
                .findFirst();
        if (encontrado.isEmpty() || cuenta.isEmpty()) {
            return Optional.empty();
        }
        Cliente cliente = encontrado.get();

        Empleado empleado = new Empleado();
        empleado.id_empleado = siguienteEmpleadoId++;
        empleado.nombre = cliente.nombre;
        empleado.apellido = cliente.apellido;
        empleado.correo = cliente.correo;
        empleado.telefono = cliente.telefono;
        empleado.documento = documento;
        empleado.salario = salario == null ? BigDecimal.ZERO : salario;
        empleado.fecha_contratacion = java.time.LocalDate.now();
        empleado.estado_empleado = "activo";
        empleado.id_rol = idRol;
        empleados.add(empleado);
        completarRol(empleado);

        Usuario usuario = cuenta.get();
        usuario.id_cliente = null;
        usuario.id_empleado = empleado.id_empleado;
        usuario.id_rol = idRol;
        cliente.estado_cliente = "inactivo";
        return Optional.of(empleado);
    }

    @Override
    public synchronized boolean actualizarEmpleado(int idEmpleado, String nombre,
                                                   BigDecimal salario, String estado) {
        Optional<Empleado> encontrado = empleados.stream()
                .filter(empleado -> empleado.id_empleado == idEmpleado)
                .findFirst();
        if (encontrado.isEmpty()) {
            return false;
        }
        Empleado empleado = encontrado.get();
        if (nombre != null && !nombre.isBlank()) {
            empleado.nombre = nombre;
        }
        if (salario != null) {
            empleado.salario = salario;
        }
        if (estado != null && !estado.isBlank()) {
            empleado.estado_empleado = estado;
        }
        return true;
    }

    @Override
    public synchronized boolean editarEmpleado(int idEmpleado, String nombre, String apellido,
                                               String documento, String telefono, BigDecimal salario,
                                               int idRol) {
        Optional<Empleado> encontrado = buscarEmpleado(idEmpleado);
        if (encontrado.isEmpty()) {
            return false;
        }
        Empleado empleado = encontrado.get();
        if (nombre != null && !nombre.isBlank()) {
            empleado.nombre = nombre;
        }
        if (apellido != null && !apellido.isBlank()) {
            empleado.apellido = apellido;
        }
        empleado.documento = documento == null || documento.isBlank() ? null : documento;
        empleado.telefono = telefono == null || telefono.isBlank() ? null : telefono;
        if (salario != null) {
            empleado.salario = salario;
        }
        empleado.id_rol = idRol;
        completarRol(empleado);
        usuarios.stream()
                .filter(usuario -> usuario.id_empleado != null && usuario.id_empleado == idEmpleado)
                .forEach(usuario -> usuario.id_rol = idRol);
        return true;
    }

    @Override
    public synchronized boolean desactivarEmpleado(int idEmpleado) {
        return actualizarEmpleado(idEmpleado, null, null, "inactivo");
    }

    @Override
    public synchronized Optional<Empleado> buscarEmpleado(int idEmpleado) {
        return empleados.stream()
                .filter(empleado -> empleado.id_empleado == idEmpleado)
                .findFirst();
    }

    private boolean mismoCorreo(String correoGuardado, String buscado) {
        return correoGuardado != null
                && correoGuardado.trim().toLowerCase(Locale.ROOT).equals(buscado);
    }

    private String nombreRol(int idRol) {
        return roles.stream()
                .filter(rol -> rol.id_rol == idRol)
                .map(rol -> rol.nombre_rol)
                .findFirst()
                .orElse("cliente");
    }

    private void completarRol(Empleado empleado) {
        empleado.nombre_rol = nombreRol(empleado.id_rol);
    }
}
