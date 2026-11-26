package com.example.demo.Persistencia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.Empleado;
import com.example.demo.Modelo.dto.SesionUsuario;

public interface PersonaRepositorio {

    Optional<SesionUsuario> autenticar(String correo, String contrasena);

    boolean existeCorreo(String correo);

    Cliente registrarCliente(Cliente cliente, String contrasena);

    Optional<String> solicitarToken(String correo);

    boolean cambiarContrasena(String token, String nuevaContrasena);

    List<Cliente> listarClientes();

    Optional<Cliente> buscarCliente(int idCliente);

    boolean cambiarEstadoCliente(int idCliente, String estado);

    List<Empleado> listarEmpleadosActivos();

    Empleado crearEmpleado(Empleado empleado, String contrasena);

    Optional<Empleado> convertirClienteEnEmpleado(int idCliente, int idRol, BigDecimal salario,
                                                  String documento);

    boolean actualizarEmpleado(int idEmpleado, String nombre, BigDecimal salario, String estado);

    boolean editarEmpleado(int idEmpleado, String nombre, String apellido, String documento,
                           String telefono, BigDecimal salario, int idRol);

    boolean desactivarEmpleado(int idEmpleado);

    Optional<Empleado> buscarEmpleado(int idEmpleado);
}
