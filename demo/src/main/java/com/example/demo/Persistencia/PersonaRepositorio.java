package com.example.demo.Persistencia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.Empleado;
import com.example.demo.Modelo.dto.SesionUsuario;

/**
 * PersonaRepositorio — capa 2 (Persistencia), INTERFAZ.
 *
 * <p>Agrupa las tres tablas que en el diseno original siempre van juntas por el
 * correo: {@code Cliente}, {@code Empleado} y {@code Usuario}. {@code login.php} las
 * cruzaba con dos JOIN solo para resolver "quien soy y que rol tengo"; aqui eso es un
 * metodo ({@link #autenticar(String, String)}).</p>
 *
 * <p>Capa: Persistencia. No sabe que existe HTTP ni Thymeleaf.</p>
 */
public interface PersonaRepositorio {

    /**
     * Verifica correo + contrasena y devuelve la sesion (con id_entidad y rol), o
     * vacio si no coincide. Busca primero en clientes y luego en empleados, igual
     * que el PHP.
     */
    Optional<SesionUsuario> autenticar(String correo, String contrasena);

    /** true si el correo ya esta tomado (por un cliente o por un empleado). */
    boolean existeCorreo(String correo);

    /** Da de alta el cliente y su cuenta con rol cliente. Devuelve el cliente creado. */
    Cliente registrarCliente(Cliente cliente, String contrasena);

    /**
     * Genera un token de recuperacion de una hora para ese correo.
     * Vacio si el correo no existe: el controlador responde igual de bien en los dos
     * casos para no revelar que correos estan registrados.
     */
    Optional<String> solicitarToken(String correo);

    /** Cambia la contrasena si el token existe y no ha vencido. */
    boolean cambiarContrasena(String token, String nuevaContrasena);

    /** Clientes con su total de pedidos, del mas reciente al mas antiguo. */
    List<Cliente> listarClientes();

    Optional<Cliente> buscarCliente(int idCliente);

    boolean cambiarEstadoCliente(int idCliente, String estado);

    /** Empleados con estado "activo" y el nombre de su rol. */
    List<Empleado> listarEmpleadosActivos();

    /**
     * Crea el empleado y su cuenta de acceso (fila en usuario con el mismo rol),
     * para que pueda iniciar sesion con su correo y la contrasena inicial.
     */
    Empleado crearEmpleado(Empleado empleado, String contrasena);

    boolean actualizarEmpleado(int idEmpleado, String nombre, BigDecimal salario, String estado);

    boolean desactivarEmpleado(int idEmpleado);

    /** Un empleado por id, para resolver el nombre del domiciliario de un envio. */
    Optional<Empleado> buscarEmpleado(int idEmpleado);
}
