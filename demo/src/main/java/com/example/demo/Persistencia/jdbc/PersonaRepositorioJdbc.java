package com.example.demo.Persistencia.jdbc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.Empleado;
import com.example.demo.Modelo.dto.SesionUsuario;
import com.example.demo.Persistencia.PersonaRepositorio;

/**
 * PersonaRepositorioJdbc — capa 2 (Persistencia), cuentas, clientes y personal en MySQL.
 *
 * <p>Las contrasenas se guardan con BCrypt ({@code usuario.contrasena_hash}). El token de
 * recuperacion se entrega una sola vez a quien lo pide y en la base queda su SHA-256: si
 * alguien lee la tabla no puede usar los tokens vigentes.</p>
 *
 * <p>Capa: Persistencia (JDBC). Activo en cualquier perfil distinto de {@code memoria}.</p>
 */
@Repository
@Profile("!memoria")
public class PersonaRepositorioJdbc implements PersonaRepositorio {

    private static final int ROL_CLIENTE = 1;

    private static final String SELECT_CLIENTE = """
            SELECT c.id_cliente, c.nombre, c.apellido, c.correo, c.telefono, c.direccion,
                   c.fecha_registro, c.estado_cliente,
                   (SELECT COUNT(*) FROM pedido p WHERE p.id_cliente = c.id_cliente) AS total_pedidos
              FROM cliente c
            """;

    private static final String SELECT_EMPLEADO = """
            SELECT e.id_empleado, e.nombre, e.apellido, e.documento, e.telefono, e.correo,
                   e.salario, e.fecha_contratacion, e.estado_empleado, e.id_rol, r.nombre_rol
              FROM empleado e
              JOIN rol r ON r.id_rol = e.id_rol
            """;

    private final JdbcClient jdbc;
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

    public PersonaRepositorioJdbc(JdbcTemplate plantilla) {
        this.jdbc = JdbcClient.create(plantilla);
    }

    // ------------------------------------------------------------------
    // Acceso
    // ------------------------------------------------------------------

    @Override
    public Optional<SesionUsuario> autenticar(String correo, String contrasena) {
        if (correo == null || contrasena == null) {
            return Optional.empty();
        }
        return jdbc.sql("""
                SELECT u.id_usuario, u.id_rol, u.contrasena_hash, r.nombre_rol,
                       COALESCE(u.id_cliente, u.id_empleado) AS id_entidad,
                       COALESCE(c.nombre, e.nombre)          AS nombre,
                       COALESCE(c.apellido, e.apellido)      AS apellido,
                       COALESCE(c.correo, e.correo, u.correo) AS correo
                  FROM usuario u
                  JOIN rol r ON r.id_rol = u.id_rol
                  LEFT JOIN cliente c  ON c.id_cliente  = u.id_cliente
                  LEFT JOIN empleado e ON e.id_empleado = u.id_empleado
                 WHERE u.correo = :correo
                   AND COALESCE(c.estado_cliente, e.estado_empleado) = 'activo'
                """)
                .param("correo", normalizar(correo))
                .query((rs, fila) -> new Credencial(rs.getString("contrasena_hash"), sesion(rs)))
                .optional()
                .filter(credencial -> codificador.matches(contrasena, credencial.hash()))
                .map(Credencial::sesion);
    }

    /** Hash guardado + sesion ya armada; la clave se compara fuera de la consulta. */
    private record Credencial(String hash, SesionUsuario sesion) {
    }

    @Override
    public boolean existeCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        Integer total = jdbc.sql("""
                SELECT COUNT(*) FROM (
                    SELECT correo FROM usuario  WHERE correo = :correo
                    UNION ALL
                    SELECT correo FROM cliente  WHERE correo = :correo
                    UNION ALL
                    SELECT correo FROM empleado WHERE correo = :correo
                ) coincidencias
                """)
                .param("correo", normalizar(correo))
                .query(Integer.class).single();
        return total != null && total > 0;
    }

    @Override
    @Transactional
    public Cliente registrarCliente(Cliente cliente, String contrasena) {
        LocalDateTime ahora = LocalDateTime.now();
        cliente.correo = normalizar(cliente.correo);
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO cliente (nombre, apellido, correo, telefono, direccion,
                                     fecha_registro, estado_cliente)
                VALUES (:nombre, :apellido, :correo, :telefono, :direccion, :fecha, 'activo')
                """)
                .param("nombre", cliente.nombre)
                .param("apellido", cliente.apellido)
                .param("correo", cliente.correo)
                .param("telefono", cliente.telefono)
                .param("direccion", cliente.direccion)
                .param("fecha", ahora)
                .update(llave);
        cliente.id_cliente = llave.getKey().intValue();
        cliente.fecha_registro = ahora;
        cliente.estado_cliente = "activo";

        jdbc.sql("""
                INSERT INTO usuario (id_rol, id_cliente, correo, contrasena_hash)
                VALUES (:rol, :cliente, :correo, :hash)
                """)
                .param("rol", ROL_CLIENTE)
                .param("cliente", cliente.id_cliente)
                .param("correo", cliente.correo)
                .param("hash", codificador.encode(contrasena))
                .update();
        return cliente;
    }

    @Override
    public Optional<String> solicitarToken(String correo) {
        if (correo == null || correo.isBlank()) {
            return Optional.empty();
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        int filas = jdbc.sql("""
                UPDATE usuario
                   SET token_reset_hash = :hash, token_expira = :expira
                 WHERE correo = :correo
                """)
                .param("hash", sha256(token))
                .param("expira", LocalDateTime.now().plusHours(1))
                .param("correo", normalizar(correo))
                .update();
        return filas > 0 ? Optional.of(token) : Optional.empty();
    }

    @Override
    public boolean cambiarContrasena(String token, String nuevaContrasena) {
        if (token == null || token.isBlank() || nuevaContrasena == null || nuevaContrasena.isBlank()) {
            return false;
        }
        return jdbc.sql("""
                UPDATE usuario
                   SET contrasena_hash = :hash, token_reset_hash = NULL, token_expira = NULL
                 WHERE token_reset_hash = :token
                   AND (token_expira IS NULL OR token_expira > :ahora)
                """)
                .param("hash", codificador.encode(nuevaContrasena))
                .param("token", sha256(token.trim()))
                .param("ahora", LocalDateTime.now())
                .update() > 0;
    }

    // ------------------------------------------------------------------
    // Clientes
    // ------------------------------------------------------------------

    @Override
    public List<Cliente> listarClientes() {
        return jdbc.sql(SELECT_CLIENTE + " ORDER BY c.fecha_registro DESC, c.id_cliente DESC")
                .query(PersonaRepositorioJdbc::cliente).list();
    }

    @Override
    public Optional<Cliente> buscarCliente(int idCliente) {
        return jdbc.sql(SELECT_CLIENTE + " WHERE c.id_cliente = :id")
                .param("id", idCliente)
                .query(PersonaRepositorioJdbc::cliente).optional();
    }

    @Override
    public boolean cambiarEstadoCliente(int idCliente, String estado) {
        if (estado == null || estado.isBlank()) {
            return false;
        }
        return jdbc.sql("UPDATE cliente SET estado_cliente = :estado WHERE id_cliente = :id")
                .param("estado", estado)
                .param("id", idCliente)
                .update() > 0;
    }

    // ------------------------------------------------------------------
    // Personal
    // ------------------------------------------------------------------

    @Override
    public List<Empleado> listarEmpleadosActivos() {
        return jdbc.sql(SELECT_EMPLEADO + " WHERE e.estado_empleado = 'activo' ORDER BY e.nombre")
                .query(PersonaRepositorioJdbc::empleado).list();
    }

    @Override
    @Transactional
    public Empleado crearEmpleado(Empleado empleado) {
        String estado = empleado.estado_empleado == null || empleado.estado_empleado.isBlank()
                ? "activo" : empleado.estado_empleado;
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO empleado (nombre, apellido, documento, telefono, correo, salario,
                                      fecha_contratacion, estado_empleado, id_rol)
                VALUES (:nombre, :apellido, :documento, :telefono, :correo, :salario,
                        :contratacion, :estado, :rol)
                """)
                .param("nombre", empleado.nombre)
                .param("apellido", empleado.apellido)
                .param("documento", empleado.documento)
                .param("telefono", empleado.telefono)
                .param("correo", normalizar(empleado.correo))
                .param("salario", empleado.salario == null ? BigDecimal.ZERO : empleado.salario)
                .param("contratacion", empleado.fecha_contratacion == null
                        ? LocalDate.now() : empleado.fecha_contratacion)
                .param("estado", estado)
                .param("rol", empleado.id_rol)
                .update(llave);
        return buscarEmpleado(llave.getKey().intValue()).orElseThrow();
    }

    @Override
    public boolean actualizarEmpleado(int idEmpleado, String nombre, BigDecimal salario, String estado) {
        return jdbc.sql("""
                UPDATE empleado
                   SET nombre          = COALESCE(:nombre, nombre),
                       salario         = COALESCE(:salario, salario),
                       estado_empleado = COALESCE(:estado, estado_empleado)
                 WHERE id_empleado = :id
                """)
                .param("nombre", nombre == null || nombre.isBlank() ? null : nombre)
                .param("salario", salario)
                .param("estado", estado == null || estado.isBlank() ? null : estado)
                .param("id", idEmpleado)
                .update() > 0;
    }

    @Override
    public boolean desactivarEmpleado(int idEmpleado) {
        return actualizarEmpleado(idEmpleado, null, null, "inactivo");
    }

    @Override
    public Optional<Empleado> buscarEmpleado(int idEmpleado) {
        return jdbc.sql(SELECT_EMPLEADO + " WHERE e.id_empleado = :id")
                .param("id", idEmpleado)
                .query(PersonaRepositorioJdbc::empleado).optional();
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String texto) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    private static SesionUsuario sesion(ResultSet rs) throws SQLException {
        SesionUsuario s = new SesionUsuario();
        s.id_usuario = rs.getInt("id_usuario");
        s.id_rol = rs.getInt("id_rol");
        s.nombre_rol = rs.getString("nombre_rol");
        s.id_entidad = rs.getInt("id_entidad");
        s.nombre = rs.getString("nombre");
        s.apellido = rs.getString("apellido");
        s.correo = rs.getString("correo");
        return s;
    }

    private static Cliente cliente(ResultSet rs, int fila) throws SQLException {
        Cliente c = new Cliente();
        c.id_cliente = rs.getInt("id_cliente");
        c.nombre = rs.getString("nombre");
        c.apellido = rs.getString("apellido");
        c.correo = rs.getString("correo");
        c.telefono = rs.getString("telefono");
        c.direccion = rs.getString("direccion");
        c.fecha_registro = Filas.fechaHora(rs, "fecha_registro");
        c.estado_cliente = rs.getString("estado_cliente");
        c.total_pedidos = rs.getInt("total_pedidos");
        return c;
    }

    private static Empleado empleado(ResultSet rs, int fila) throws SQLException {
        Empleado e = new Empleado();
        e.id_empleado = rs.getInt("id_empleado");
        e.nombre = rs.getString("nombre");
        e.apellido = rs.getString("apellido");
        e.documento = rs.getString("documento");
        e.telefono = rs.getString("telefono");
        e.correo = rs.getString("correo");
        e.salario = rs.getBigDecimal("salario");
        e.fecha_contratacion = Filas.fecha(rs, "fecha_contratacion");
        e.estado_empleado = rs.getString("estado_empleado");
        e.id_rol = rs.getInt("id_rol");
        e.nombre_rol = rs.getString("nombre_rol");
        return e;
    }
}
