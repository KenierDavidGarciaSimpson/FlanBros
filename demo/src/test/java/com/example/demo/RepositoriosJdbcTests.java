package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.Producto;
import com.example.demo.Modelo.dto.ItemPedido;
import com.example.demo.Modelo.dto.NuevoPedido;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Persistencia.PersonaRepositorio;
import com.example.demo.Persistencia.ProductoRepositorio;
import com.example.demo.Persistencia.StockInsuficienteException;

/**
 * Pruebas de los repositorios JDBC contra MySQL REAL (no mocks): transacciones, restricciones
 * de la base, BCrypt y tokens de recuperacion.
 *
 * <p>Usan una base aparte, {@code flanbross_test}, que se crea sola. Antes de cada prueba se hace
 * {@code flyway.clean()} + {@code migrate()}, asi que cada una parte de los datos demo y no
 * depende de las demas. Si no hay MySQL en localhost:3306 (por ejemplo {@code docker compose up -d}),
 * toda la clase se OMITE en lugar de fallar.</p>
 *
 * <p>Credenciales por defecto: root/root (las del docker-compose). Se cambian con las variables
 * DB_TEST_URL, DB_TEST_USUARIO y DB_TEST_CLAVE.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${DB_TEST_URL:jdbc:mysql://localhost:3306/flanbross_test"
                + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true}",
        "spring.datasource.username=${DB_TEST_USUARIO:root}",
        "spring.datasource.password=${DB_TEST_CLAVE:root}",
        "spring.flyway.clean-disabled=false" })
@EnabledIf("mysqlDisponible")
class RepositoriosJdbcTests {

    static boolean mysqlDisponible() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 3306), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PedidoRepositorio pedidos;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private PersonaRepositorio personas;

    @BeforeEach
    void restaurarDatosDemo() {
        flyway.clean();
        flyway.migrate();
    }

    // ---------------- Utilidades ----------------

    private int stock(int idProducto) {
        return jdbc.queryForObject(
                "SELECT cantidad_disponible FROM inventario WHERE id_producto = ?", Integer.class, idProducto);
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private static ItemPedido linea(int idProducto, int cantidad) {
        ItemPedido item = new ItemPedido();
        item.id_producto = idProducto;
        item.cantidad = cantidad;
        return item;
    }

    private static NuevoPedido solicitud(int idCliente, ItemPedido... lineas) {
        NuevoPedido nuevo = new NuevoPedido();
        nuevo.id_cliente = idCliente;
        nuevo.direccion_entrega = "Bocagrande, Calle 5 #2-15";
        nuevo.metodo_pago = "tarjeta";
        nuevo.items = new ArrayList<>(java.util.List.of(lineas));
        return nuevo;
    }

    // ---------------- Pedidos ----------------

    @Test
    void crearPedidoDescuentaStockYEmiteFacturaPagoYDomicilio() {
        int stockAntes = stock(3); // brownie, $8.500
        Pedido pedido = pedidos.crear(solicitud(1, linea(3, 2), linea(10, 1))); // 2 x 8.500 + 1 x 19.000

        assertEquals(0, new BigDecimal("36000").compareTo(pedido.total_pedido));
        assertEquals(0, new BigDecimal("38880").compareTo(pedido.total_factura), "IVA 8 %: 36.000 + 2.880");
        assertTrue(pedido.numero_factura.startsWith("FAC-"));
        assertEquals(2, pedido.detalles.size());
        assertEquals(stockAntes - 2, stock(3));

        assertEquals("pendiente", jdbc.queryForObject(
                "SELECT g.estado_pago FROM pago g JOIN factura f ON f.id_factura = g.id_factura WHERE f.id_pedido = ?",
                String.class, pedido.id_pedido));
        assertEquals(0, new BigDecimal("7000").compareTo(jdbc.queryForObject(
                "SELECT costo_domicilio FROM domicilio WHERE id_pedido = ?", BigDecimal.class, pedido.id_pedido)));
    }

    @Test
    void siFallaUnaLineaNoQuedaNadaAMedias() {
        // El producto 5 tiene 3 unidades. Cada linea (2) pasa la validacion por separado, pero la
        // segunda resta ya no alcanza: el descuento atomico falla DESPUES de escribir pedido y lineas.
        int pedidosAntes = contar("pedido");
        int detallesAntes = contar("detalle_pedido");
        int facturasAntes = contar("factura");
        int stockAntes = stock(5);

        assertThrows(StockInsuficienteException.class,
                () -> pedidos.crear(solicitud(1, linea(5, 2), linea(5, 2))));

        assertEquals(pedidosAntes, contar("pedido"), "el pedido debe revertirse");
        assertEquals(detallesAntes, contar("detalle_pedido"), "las lineas deben revertirse");
        assertEquals(facturasAntes, contar("factura"), "la factura no debe existir");
        assertEquals(stockAntes, stock(5), "el stock descontado en la primera linea debe volver");
    }

    @Test
    void pedirMasDeLoQueHayEsRechazadoSinCambios() {
        int pedidosAntes = contar("pedido");
        assertThrows(StockInsuficienteException.class, () -> pedidos.crear(solicitud(1, linea(5, 999))));
        assertEquals(pedidosAntes, contar("pedido"));
        assertEquals(3, stock(5));
    }

    @Test
    void entregarUnPedidoPagaLaFacturaYApruebaElPago() {
        Pedido pedido = pedidos.crear(solicitud(1, linea(3, 1)));

        assertFalse(pedidos.cambiarEstado(pedido.id_pedido, "volando"), "estado inventado");
        assertTrue(pedidos.cambiarEstado(pedido.id_pedido, "entregado"));

        assertEquals("pagada", jdbc.queryForObject(
                "SELECT estado_factura FROM factura WHERE id_pedido = ?", String.class, pedido.id_pedido));
        assertEquals("aprobado", jdbc.queryForObject(
                "SELECT g.estado_pago FROM pago g JOIN factura f ON f.id_factura = g.id_factura WHERE f.id_pedido = ?",
                String.class, pedido.id_pedido));
        assertEquals("entregado", jdbc.queryForObject(
                "SELECT estado_entrega FROM domicilio WHERE id_pedido = ?", String.class, pedido.id_pedido));
        assertNotNull(jdbc.queryForObject(
                "SELECT fecha_entrega FROM domicilio WHERE id_pedido = ?", java.sql.Timestamp.class, pedido.id_pedido));
    }

    @Test
    void cancelarUnPedidoMarcaElDomicilioComoFallido() {
        Pedido pedido = pedidos.crear(solicitud(1, linea(3, 1)));
        assertTrue(pedidos.cambiarEstado(pedido.id_pedido, "cancelado"));
        assertEquals("fallido", jdbc.queryForObject(
                "SELECT estado_entrega FROM domicilio WHERE id_pedido = ?", String.class, pedido.id_pedido));
    }

    // ---------------- Inventario y catalogo ----------------

    @Test
    void elDescuentoDeInventarioNuncaDejaStockNegativo() {
        assertFalse(productos.descontarInventario(5, 99));
        assertEquals(3, stock(5));
        assertTrue(productos.descontarInventario(5, 3));
        assertEquals(0, stock(5));
        assertFalse(productos.descontarInventario(5, 1), "ya no queda stock");
    }

    @Test
    void crearProductoCreaLaCategoriaNuevaYSuInventario() {
        Producto nuevo = new Producto();
        nuevo.nombre_producto = "Torta de Prueba";
        nuevo.descripcion = "Solo para la prueba";
        nuevo.precio = new BigDecimal("25000");
        nuevo.stock = 7;
        nuevo.categoria = "Categoria Nueva";

        Producto creado = productos.crear(nuevo);

        assertTrue(creado.id_producto > 0);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM categoria WHERE nombre = 'Categoria Nueva'", Integer.class));
        assertEquals(7, productos.buscarInventario(creado.id_producto).orElseThrow().cantidad_disponible);
        assertEquals("Categoria Nueva", productos.buscarPorId(creado.id_producto).orElseThrow().categoria);
    }

    // ---------------- Cuentas ----------------

    @Test
    void loginUsaBcryptYBloqueaCuentasInactivas() {
        assertTrue(personas.autenticar("laura@demo.com", "demo1234").isPresent());
        assertTrue(personas.autenticar("LAURA@demo.com ", "demo1234").isPresent(), "ignora mayusculas y espacios");
        assertTrue(personas.autenticar("laura@demo.com", "incorrecta").isEmpty());
        assertTrue(personas.autenticar("noexiste@demo.com", "demo1234").isEmpty());

        assertTrue(jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE correo = 'laura@demo.com'",
                String.class).startsWith("$2"), "la contrasena debe estar como hash BCrypt");

        assertTrue(personas.cambiarEstadoCliente(1, "inactivo"));
        assertTrue(personas.autenticar("laura@demo.com", "demo1234").isEmpty(), "cuenta inactiva");
    }

    @Test
    void registrarClienteGuardaSoloElHash() {
        Cliente nuevo = new Cliente();
        nuevo.nombre = "Nuevo";
        nuevo.apellido = "Cliente";
        nuevo.correo = "nuevo@demo.com";

        personas.registrarCliente(nuevo, "ClaveSegura123");

        String hash = jdbc.queryForObject(
                "SELECT contrasena_hash FROM usuario WHERE correo = 'nuevo@demo.com'", String.class);
        assertTrue(hash.startsWith("$2") && !hash.contains("ClaveSegura123"));
        assertTrue(personas.autenticar("nuevo@demo.com", "ClaveSegura123").isPresent());
        assertTrue(personas.existeCorreo("NUEVO@demo.com"));
    }

    @Test
    void elTokenDeRecuperacionSeGuardaCifradoYSirveUnaSolaVez() {
        String token = personas.solicitarToken("laura@demo.com").orElseThrow();

        String enBase = jdbc.queryForObject(
                "SELECT token_reset_hash FROM usuario WHERE correo = 'laura@demo.com'", String.class);
        assertEquals(64, enBase.length(), "SHA-256 en hexadecimal");
        assertFalse(enBase.equals(token), "en la base no debe estar el token en claro");

        assertTrue(personas.cambiarContrasena(token, "OtraClave12345"));
        assertFalse(personas.cambiarContrasena(token, "TercerIntento1"), "el token es de un solo uso");
        assertTrue(personas.autenticar("laura@demo.com", "OtraClave12345").isPresent());
        assertTrue(personas.autenticar("laura@demo.com", "demo1234").isEmpty());
        assertTrue(personas.solicitarToken("noexiste@demo.com").isEmpty());
    }

    // ---------------- Restricciones de la base ----------------

    @Test
    void laBaseRechazaDatosInvalidosAunqueFalleLaAplicacion() {
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO pedido (estado_pedido, total_pedido, id_cliente) VALUES ('volando', 1, 1)"));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO pedido (estado_pedido, total_pedido, id_cliente) VALUES ('pendiente', 1, 999)"));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE inventario SET cantidad_disponible = -1 WHERE id_producto = 1"));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO usuario (id_rol, correo, contrasena_hash) VALUES (1, 'huerfano@demo.com', 'x')"));
    }
}
