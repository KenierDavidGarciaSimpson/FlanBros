package com.example.demo.Persistencia.jdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Modelo.DetallePedido;
import com.example.demo.Modelo.Domicilio;
import com.example.demo.Modelo.Factura;
import com.example.demo.Modelo.Pago;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.Producto;
import com.example.demo.Modelo.dto.ItemPedido;
import com.example.demo.Modelo.dto.NuevoPedido;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Persistencia.ProductoRepositorio;
import com.example.demo.Persistencia.StockInsuficienteException;

/**
 * PedidoRepositorioJdbc — capa 2 (Persistencia), pedidos, facturas, pagos y domicilios en PostgreSQL.
 *
 * <p>Conserva las reglas del repositorio en memoria (IVA 8 %, domicilio $7.000, factura
 * {@code FAC-AAAA-000N}, cascada al cambiar el estado del pedido) pero ahora cada operacion
 * que toca varias tablas corre en <b>una sola transaccion</b>: si falta stock en la tercera
 * linea, el pedido, la factura, el pago y el domicilio no se guardan.</p>
 *
 * <p>Capa: Persistencia (JDBC). Activo en cualquier perfil distinto de {@code memoria}.</p>
 */
@Repository
@Profile("!memoria")
public class PedidoRepositorioJdbc implements PedidoRepositorio {

    private static final BigDecimal TASA_IVA = new BigDecimal("0.08");
    private static final BigDecimal COSTO_DOMICILIO = new BigDecimal("7000");
    private static final List<String> METODOS_PAGO = List.of(
            "contraentrega", "tarjeta", "transferencia", "billetera_digital");
    private static final List<String> ESTADOS_PEDIDO = List.of(
            "pendiente", "en_preparacion", "listo", "entregado", "cancelado");
    private static final List<String> ESTADOS_PAGO = List.of(
            "pendiente", "aprobado", "rechazado");

    private static final String SELECT_PEDIDO = """
            SELECT p.id_pedido, p.fecha_pedido, p.estado_pedido, p.total_pedido, p.observaciones,
                   p.id_cliente, c.nombre, c.apellido, c.correo,
                   f.numero_factura, f.total_factura, f.estado_factura
              FROM pedido p
              JOIN cliente c ON c.id_cliente = p.id_cliente
              LEFT JOIN factura f ON f.id_pedido = p.id_pedido
            """;

    private static final String SELECT_DETALLE = """
            SELECT d.id_detalle, d.id_pedido, d.id_producto, d.cantidad, d.precio_unitario,
                   d.subtotal, pr.nombre_producto
              FROM detalle_pedido d
              JOIN producto pr ON pr.id_producto = d.id_producto
            """;

    private static final String SELECT_FACTURA = """
            SELECT f.id_factura, f.numero_factura, f.fecha_factura, f.subtotal, f.impuestos,
                   f.total_factura, f.estado_factura, f.id_cliente, f.id_pedido,
                   c.nombre, c.apellido, c.correo, p.estado_pedido
              FROM factura f
              JOIN cliente c ON c.id_cliente = f.id_cliente
              JOIN pedido p ON p.id_pedido = f.id_pedido
            """;

    private static final String SELECT_PAGO = """
            SELECT g.id_pago, g.fecha_pago, g.monto_pago, g.metodo_pago, g.estado_pago,
                   g.referencia_pago, g.id_factura, f.numero_factura, f.total_factura,
                   c.nombre, c.apellido, c.correo
              FROM pago g
              JOIN factura f ON f.id_factura = g.id_factura
              JOIN cliente c ON c.id_cliente = f.id_cliente
            """;

    private static final String SELECT_DOMICILIO = """
            SELECT d.id_domicilio, d.direccion_entrega, d.fecha_entrega, d.estado_entrega,
                   d.costo_domicilio, d.id_pedido, d.id_domiciliario,
                   p.estado_pedido, p.total_pedido, c.nombre, c.apellido, c.telefono,
                   f.numero_factura, e.nombre AS nombre_domiciliario
              FROM domicilio d
              JOIN pedido p ON p.id_pedido = d.id_pedido
              JOIN cliente c ON c.id_cliente = p.id_cliente
              LEFT JOIN factura f ON f.id_pedido = p.id_pedido
              LEFT JOIN empleado e ON e.id_empleado = d.id_domiciliario
            """;

    private final JdbcClient jdbc;
    private final ProductoRepositorio productos;

    public PedidoRepositorioJdbc(JdbcTemplate plantilla, ProductoRepositorio productos) {
        this.jdbc = JdbcClient.create(plantilla);
        this.productos = productos;
    }

    // ------------------------------------------------------------------
    // Crear pedido (una sola transaccion)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Pedido crear(NuevoPedido solicitud) {
        // ---- 1. Validar TODAS las lineas antes de escribir nada -------------
        List<Producto> productosPedidos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : solicitud.items) {
            if (!item.esValido()) {
                throw new StockInsuficienteException(
                        "Hay una linea del pedido con producto o cantidad invalidos.");
            }
            Producto producto = productos.buscarPorId(item.id_producto)
                    .orElseThrow(() -> new StockInsuficienteException(
                            "El producto ID " + item.id_producto + " no existe."));
            if (!producto.estaDisponible()) {
                throw new StockInsuficienteException(
                        "El producto '" + producto.nombre_producto + "' no esta disponible.");
            }
            int disponible = disponibleDe(item.id_producto, producto);
            if (disponible < item.cantidad) {
                throw StockInsuficienteException.para(item.id_producto, disponible, item.cantidad);
            }
            productosPedidos.add(producto);
            // El precio bueno es el del producto, no el que manda el navegador.
            total = total.add(producto.precio.multiply(BigDecimal.valueOf(item.cantidad)));
        }

        // ---- 1b. Cupon (el controlador ya lo valido contra la tabla cupon) ---
        String observaciones = solicitud.observaciones;
        if (solicitud.descuento_pct > 0) {
            BigDecimal descuento = total.multiply(BigDecimal.valueOf(solicitud.descuento_pct))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            total = total.subtract(descuento);
            String nota = "Cupón " + solicitud.cupon_aplicado + " (-" + solicitud.descuento_pct
                    + "%: $" + descuento.toPlainString() + ")";
            observaciones = observaciones == null ? nota : observaciones + " | " + nota;
        }

        // ---- 2. Cabecera del pedido ----------------------------------------
        LocalDateTime ahora = LocalDateTime.now();
        KeyHolder llavePedido = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO pedido (fecha_pedido, estado_pedido, total_pedido, observaciones, id_cliente)
                VALUES (:fecha, 'pendiente', :total, :observaciones, :cliente)
                """)
                .param("fecha", ahora)
                .param("total", total)
                .param("observaciones", observaciones)
                .param("cliente", solicitud.id_cliente)
                .update(llavePedido, "id_pedido");
        int idPedido = llavePedido.getKey().intValue();

        // ---- 3. Lineas y descuento atomico de inventario ---------------------
        for (int i = 0; i < solicitud.items.size(); i++) {
            ItemPedido item = solicitud.items.get(i);
            Producto producto = productosPedidos.get(i);
            jdbc.sql("""
                    INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
                    VALUES (:pedido, :producto, :cantidad, :precio, :subtotal)
                    """)
                    .param("pedido", idPedido)
                    .param("producto", producto.id_producto)
                    .param("cantidad", item.cantidad)
                    .param("precio", producto.precio)
                    .param("subtotal", producto.precio.multiply(BigDecimal.valueOf(item.cantidad)))
                    .update();
            if (!productos.descontarInventario(producto.id_producto, item.cantidad)) {
                // RuntimeException: Spring deshace toda la transaccion (pedido, lineas y descuentos).
                throw StockInsuficienteException.para(producto.id_producto,
                        disponibleDe(producto.id_producto, producto), item.cantidad);
            }
        }

        // ---- 4. Factura (subtotal + IVA) -------------------------------------
        BigDecimal impuestos = total.multiply(TASA_IVA).setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalFactura = total.add(impuestos);
        KeyHolder llaveFactura = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO factura (numero_factura, fecha_factura, subtotal, impuestos, total_factura,
                                     estado_factura, id_cliente, id_pedido)
                VALUES (:numero, :fecha, :subtotal, :impuestos, :total, 'pendiente', :cliente, :pedido)
                """)
                .param("numero", numeroFactura(idPedido))
                .param("fecha", ahora)
                .param("subtotal", total)
                .param("impuestos", impuestos)
                .param("total", totalFactura)
                .param("cliente", solicitud.id_cliente)
                .param("pedido", idPedido)
                .update(llaveFactura, "id_factura");
        int idFactura = llaveFactura.getKey().intValue();

        // ---- 5. Pago pendiente -----------------------------------------------
        String metodo = METODOS_PAGO.contains(solicitud.metodo_pago) ? solicitud.metodo_pago : "contraentrega";
        jdbc.sql("""
                INSERT INTO pago (fecha_pago, monto_pago, metodo_pago, estado_pago, id_factura)
                VALUES (:fecha, :monto, :metodo, 'pendiente', :factura)
                """)
                .param("fecha", ahora)
                .param("monto", totalFactura)
                .param("metodo", metodo)
                .param("factura", idFactura)
                .update();

        // ---- 6. Domicilio ----------------------------------------------------
        jdbc.sql("""
                INSERT INTO domicilio (direccion_entrega, estado_entrega, costo_domicilio, id_pedido)
                VALUES (:direccion, 'pendiente', :costo, :pedido)
                """)
                .param("direccion", solicitud.direccion_entrega)
                .param("costo", COSTO_DOMICILIO)
                .param("pedido", idPedido)
                .update();

        // ---- 7. Devolver el pedido con su factura ----------------------------
        return buscarPorId(idPedido).orElseThrow();
    }

    // ------------------------------------------------------------------
    // Cambio de estado (cascada en una sola transaccion)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public boolean cambiarEstado(int idPedido, String estado) {
        if (estado == null || !ESTADOS_PEDIDO.contains(estado.trim())) {
            return false;
        }
        String nuevoEstado = estado.trim();
        int filas = jdbc.sql("UPDATE pedido SET estado_pedido = :estado WHERE id_pedido = :id")
                .param("estado", nuevoEstado)
                .param("id", idPedido)
                .update();
        if (filas == 0) {
            return false;
        }
        // Efectos en cascada, exactamente como los hacia pedidos.php.
        switch (nuevoEstado) {
            case "listo" -> cambiarEstadoDomicilioDe(idPedido, "en_camino");
            case "entregado" -> {
                cambiarEstadoDomicilioDe(idPedido, "entregado");
                jdbc.sql("UPDATE factura SET estado_factura = 'pagada' WHERE id_pedido = :id")
                        .param("id", idPedido)
                        .update();
                jdbc.sql("""
                        UPDATE pago SET estado_pago = 'aprobado'
                         WHERE id_factura IN (SELECT id_factura FROM factura WHERE id_pedido = :id)
                        """)
                        .param("id", idPedido)
                        .update();
            }
            case "cancelado" -> cambiarEstadoDomicilioDe(idPedido, "fallido");
            default -> {
                // pendiente y en_preparacion no mueven nada mas.
            }
        }
        return true;
    }

    private void cambiarEstadoDomicilioDe(int idPedido, String estado) {
        jdbc.sql("UPDATE domicilio SET estado_entrega = :estado WHERE id_pedido = :id")
                .param("estado", estado)
                .param("id", idPedido)
                .update();
        if ("entregado".equals(estado)) {
            marcarFechaEntrega("id_pedido", idPedido);
        }
    }

    /** Sella la hora de entrega solo si todavia no la tenia. */
    private void marcarFechaEntrega(String columnaClave, int valor) {
        // columnaClave es una constante interna ("id_pedido" o "id_domicilio"), nunca entrada del usuario.
        jdbc.sql("UPDATE domicilio SET fecha_entrega = :ahora WHERE " + columnaClave
                        + " = :id AND fecha_entrega IS NULL")
                .param("ahora", LocalDateTime.now())
                .param("id", valor)
                .update();
    }

    // ------------------------------------------------------------------
    // Pedidos
    // ------------------------------------------------------------------

    @Override
    public List<Pedido> listarRecientes(int limite) {
        String tope = limite > 0 ? " LIMIT " + limite : "";
        List<Pedido> lista = jdbc.sql(SELECT_PEDIDO + " ORDER BY p.fecha_pedido DESC, p.id_pedido DESC" + tope)
                .query(PedidoRepositorioJdbc::pedido).list();
        return conDetalles(lista);
    }

    @Override
    public List<Pedido> listarPorCliente(int idCliente) {
        List<Pedido> lista = jdbc.sql(SELECT_PEDIDO
                        + " WHERE p.id_cliente = :cliente ORDER BY p.fecha_pedido DESC, p.id_pedido DESC")
                .param("cliente", idCliente)
                .query(PedidoRepositorioJdbc::pedido).list();
        return conDetalles(lista);
    }

    @Override
    public Optional<Pedido> buscarPorId(int idPedido) {
        List<Pedido> lista = jdbc.sql(SELECT_PEDIDO + " WHERE p.id_pedido = :id")
                .param("id", idPedido)
                .query(PedidoRepositorioJdbc::pedido).list();
        return conDetalles(lista).stream().findFirst();
    }

    @Override
    public List<DetallePedido> listarDetalles() {
        return jdbc.sql(SELECT_DETALLE + " ORDER BY d.id_detalle")
                .query(PedidoRepositorioJdbc::detalle).list();
    }

    @Override
    public List<DetallePedido> detallesDe(int idPedido) {
        return jdbc.sql(SELECT_DETALLE + " WHERE d.id_pedido = :id ORDER BY d.id_detalle")
                .param("id", idPedido)
                .query(PedidoRepositorioJdbc::detalle).list();
    }

    // ------------------------------------------------------------------
    // Facturas y pagos
    // ------------------------------------------------------------------

    @Override
    public List<Factura> listarFacturas() {
        return jdbc.sql(SELECT_FACTURA + " ORDER BY f.fecha_factura DESC, f.id_factura DESC")
                .query(PedidoRepositorioJdbc::factura).list();
    }

    @Override
    public List<Factura> listarFacturasPorCliente(int idCliente) {
        return jdbc.sql(SELECT_FACTURA
                        + " WHERE f.id_cliente = :cliente ORDER BY f.fecha_factura DESC, f.id_factura DESC")
                .param("cliente", idCliente)
                .query(PedidoRepositorioJdbc::factura).list();
    }

    @Override
    public List<Pago> listarPagos() {
        return jdbc.sql(SELECT_PAGO + " ORDER BY g.fecha_pago DESC, g.id_pago DESC")
                .query(PedidoRepositorioJdbc::pago).list();
    }

    @Override
    public boolean cambiarEstadoPago(int idPago, String estado) {
        if (estado == null || !ESTADOS_PAGO.contains(estado.trim())) {
            return false;
        }
        return jdbc.sql("UPDATE pago SET estado_pago = :estado WHERE id_pago = :id")
                .param("estado", estado.trim())
                .param("id", idPago)
                .update() > 0;
    }

    // ------------------------------------------------------------------
    // Domicilios
    // ------------------------------------------------------------------

    @Override
    public List<Domicilio> listarDomicilios() {
        return jdbc.sql(SELECT_DOMICILIO + " ORDER BY d.id_domicilio")
                .query(PedidoRepositorioJdbc::domicilio).list();
    }

    @Override
    public List<Domicilio> listarDomiciliosPorDomiciliario(int idEmpleado) {
        return jdbc.sql(SELECT_DOMICILIO + """
                 WHERE d.id_domiciliario = :empleado
                   AND d.estado_entrega IN ('pendiente', 'en_camino')
                 ORDER BY d.id_domicilio
                """)
                .param("empleado", idEmpleado)
                .query(PedidoRepositorioJdbc::domicilio).list();
    }

    @Override
    public Optional<Domicilio> buscarDomicilioPorPedido(int idPedido) {
        return jdbc.sql(SELECT_DOMICILIO + " WHERE d.id_pedido = :pedido")
                .param("pedido", idPedido)
                .query(PedidoRepositorioJdbc::domicilio).optional();
    }

    @Override
    @Transactional
    public boolean actualizarDomicilio(int idDomicilio, String estado, Integer idDomiciliario) {
        Integer existe = jdbc.sql("SELECT COUNT(*) FROM domicilio WHERE id_domicilio = :id")
                .param("id", idDomicilio)
                .query(Integer.class).single();
        if (existe == null || existe == 0) {
            return false;
        }
        if (estado != null && !estado.isBlank()) {
            jdbc.sql("UPDATE domicilio SET estado_entrega = :estado WHERE id_domicilio = :id")
                    .param("estado", estado.trim())
                    .param("id", idDomicilio)
                    .update();
            if ("entregado".equals(estado.trim())) {
                marcarFechaEntrega("id_domicilio", idDomicilio);
            }
        }
        if (idDomiciliario != null) {
            jdbc.sql("UPDATE domicilio SET id_domiciliario = :empleado WHERE id_domicilio = :id")
                    .param("empleado", idDomiciliario)
                    .param("id", idDomicilio)
                    .update();
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private int disponibleDe(int idProducto, Producto producto) {
        return productos.buscarInventario(idProducto)
                .map(fila -> fila.cantidad_disponible)
                .orElse(producto.stock);
    }

    private String numeroFactura(int idPedido) {
        return "FAC-" + Year.now().getValue() + "-" + String.format("%04d", idPedido);
    }

    /** Una sola consulta trae las lineas de todos los pedidos listados (evita N+1). */
    private List<Pedido> conDetalles(List<Pedido> lista) {
        if (lista.isEmpty()) {
            return lista;
        }
        List<Integer> ids = lista.stream().map(pedido -> pedido.id_pedido).toList();
        Map<Integer, List<DetallePedido>> porPedido = jdbc
                .sql(SELECT_DETALLE + " WHERE d.id_pedido IN (:ids) ORDER BY d.id_detalle")
                .param("ids", ids)
                .query(PedidoRepositorioJdbc::detalle).list()
                .stream()
                .collect(Collectors.groupingBy(linea -> linea.id_pedido));
        for (Pedido pedido : lista) {
            pedido.detalles = porPedido.getOrDefault(pedido.id_pedido, new ArrayList<>());
        }
        return lista;
    }

    private static Pedido pedido(ResultSet rs, int fila) throws SQLException {
        Pedido p = new Pedido();
        p.id_pedido = rs.getInt("id_pedido");
        p.fecha_pedido = Filas.fechaHora(rs, "fecha_pedido");
        p.estado_pedido = rs.getString("estado_pedido");
        p.total_pedido = rs.getBigDecimal("total_pedido");
        p.observaciones = rs.getString("observaciones");
        p.id_cliente = rs.getInt("id_cliente");
        p.nombre = rs.getString("nombre");
        p.apellido = rs.getString("apellido");
        p.correo = rs.getString("correo");
        p.numero_factura = rs.getString("numero_factura");
        p.total_factura = rs.getBigDecimal("total_factura");
        p.estado_factura = rs.getString("estado_factura");
        return p;
    }

    private static DetallePedido detalle(ResultSet rs, int fila) throws SQLException {
        DetallePedido d = new DetallePedido();
        d.id_detalle = rs.getInt("id_detalle");
        d.id_pedido = rs.getInt("id_pedido");
        d.id_producto = rs.getInt("id_producto");
        d.cantidad = rs.getInt("cantidad");
        d.precio_unitario = rs.getBigDecimal("precio_unitario");
        d.subtotal = rs.getBigDecimal("subtotal");
        d.nombre_producto = rs.getString("nombre_producto");
        return d;
    }

    private static Factura factura(ResultSet rs, int fila) throws SQLException {
        Factura f = new Factura();
        f.id_factura = rs.getInt("id_factura");
        f.numero_factura = rs.getString("numero_factura");
        f.fecha_factura = Filas.fechaHora(rs, "fecha_factura");
        f.subtotal = rs.getBigDecimal("subtotal");
        f.impuestos = rs.getBigDecimal("impuestos");
        f.total_factura = rs.getBigDecimal("total_factura");
        f.estado_factura = rs.getString("estado_factura");
        f.id_cliente = rs.getInt("id_cliente");
        f.id_pedido = rs.getInt("id_pedido");
        f.nombre = rs.getString("nombre");
        f.apellido = rs.getString("apellido");
        f.correo = rs.getString("correo");
        f.estado_pedido = rs.getString("estado_pedido");
        return f;
    }

    private static Pago pago(ResultSet rs, int fila) throws SQLException {
        Pago g = new Pago();
        g.id_pago = rs.getInt("id_pago");
        g.fecha_pago = Filas.fechaHora(rs, "fecha_pago");
        g.monto_pago = rs.getBigDecimal("monto_pago");
        g.metodo_pago = rs.getString("metodo_pago");
        g.estado_pago = rs.getString("estado_pago");
        g.referencia_pago = rs.getString("referencia_pago");
        g.id_factura = rs.getInt("id_factura");
        g.numero_factura = rs.getString("numero_factura");
        g.total_factura = rs.getBigDecimal("total_factura");
        g.nombre = rs.getString("nombre");
        g.apellido = rs.getString("apellido");
        g.correo = rs.getString("correo");
        return g;
    }

    private static Domicilio domicilio(ResultSet rs, int fila) throws SQLException {
        Domicilio d = new Domicilio();
        d.id_domicilio = rs.getInt("id_domicilio");
        d.direccion_entrega = rs.getString("direccion_entrega");
        d.fecha_entrega = Filas.fechaHora(rs, "fecha_entrega");
        d.estado_entrega = rs.getString("estado_entrega");
        d.costo_domicilio = rs.getBigDecimal("costo_domicilio");
        d.id_pedido = rs.getInt("id_pedido");
        d.id_domiciliario = Filas.enteroONulo(rs, "id_domiciliario");
        d.estado_pedido = rs.getString("estado_pedido");
        d.total_pedido = rs.getBigDecimal("total_pedido");
        d.nombre = rs.getString("nombre");
        d.apellido = rs.getString("apellido");
        d.telefono = rs.getString("telefono");
        d.numero_factura = rs.getString("numero_factura");
        d.nombre_domiciliario = rs.getString("nombre_domiciliario");
        return d;
    }
}
