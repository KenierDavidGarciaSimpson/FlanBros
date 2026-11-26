package com.example.demo.Persistencia.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Modelo.Inventario;
import com.example.demo.Modelo.Producto;
import com.example.demo.Persistencia.ProductoRepositorio;

@Repository
@Profile("!memoria")
public class ProductoRepositorioJdbc implements ProductoRepositorio {

    private static final int STOCK_MINIMO_POR_DEFECTO = 5;

    private static final String SELECT_PRODUCTO = """
            SELECT p.id_producto, p.nombre_producto, p.descripcion, p.precio,
                   COALESCE(i.cantidad_disponible, 0) AS stock,
                   c.nombre AS categoria, p.fecha_vencimiento, p.estado_producto,
                   p.imagen, p.emoji, p.badge
              FROM producto p
              JOIN categoria c ON c.id_categoria = p.id_categoria
              LEFT JOIN inventario i ON i.id_producto = p.id_producto
            """;

    private static final String SELECT_INVENTARIO = """
            SELECT i.id_inventario, i.id_producto, i.cantidad_disponible, i.cantidad_minima,
                   i.fecha_actualizacion, p.nombre_producto, c.nombre AS categoria, p.precio
              FROM inventario i
              JOIN producto p ON p.id_producto = i.id_producto
              JOIN categoria c ON c.id_categoria = p.id_categoria
            """;

    private final JdbcClient jdbc;

    public ProductoRepositorioJdbc(JdbcTemplate plantilla) {
        this.jdbc = JdbcClient.create(plantilla);
    }

    @Override
    public List<Producto> listarDisponibles() {
        return jdbc.sql(SELECT_PRODUCTO + " WHERE p.estado_producto = 'disponible' ORDER BY p.nombre_producto")
                .query(ProductoRepositorioJdbc::producto).list();
    }

    @Override
    public List<Producto> listarTodos() {
        return jdbc.sql(SELECT_PRODUCTO + " ORDER BY p.nombre_producto")
                .query(ProductoRepositorioJdbc::producto).list();
    }

    @Override
    public List<Producto> listarPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return listarDisponibles();
        }
        return jdbc.sql(SELECT_PRODUCTO
                        + " WHERE p.estado_producto = 'disponible' AND c.nombre = :categoria"
                        + " ORDER BY p.nombre_producto")
                .param("categoria", categoria.trim())
                .query(ProductoRepositorioJdbc::producto).list();
    }

    @Override
    public Optional<Producto> buscarPorId(int idProducto) {
        return jdbc.sql(SELECT_PRODUCTO + " WHERE p.id_producto = :id")
                .param("id", idProducto)
                .query(ProductoRepositorioJdbc::producto).optional();
    }

    @Override
    @Transactional
    public Producto crear(Producto producto) {
        String estado = producto.estado_producto == null || producto.estado_producto.isBlank()
                ? "disponible" : producto.estado_producto;
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO producto (nombre_producto, descripcion, precio, id_categoria,
                                      fecha_vencimiento, estado_producto, imagen, emoji, badge)
                VALUES (:nombre, :descripcion, :precio, :categoria,
                        :vencimiento, :estado, :imagen, :emoji, :badge)
                """)
                .param("nombre", producto.nombre_producto)
                .param("descripcion", producto.descripcion)
                .param("precio", producto.precio)
                .param("categoria", idCategoria(producto.categoria))
                .param("vencimiento", producto.fecha_vencimiento)
                .param("estado", estado)
                .param("imagen", producto.imagen)
                .param("emoji", producto.emoji)
                .param("badge", producto.badge)
                .update(llave, "id_producto");
        producto.id_producto = llave.getKey().intValue();
        producto.estado_producto = estado;

        jdbc.sql("""
                INSERT INTO inventario (id_producto, cantidad_disponible, cantidad_minima, fecha_actualizacion)
                VALUES (:id, :cantidad, :minima, :ahora)
                """)
                .param("id", producto.id_producto)
                .param("cantidad", Math.max(producto.stock, 0))
                .param("minima", STOCK_MINIMO_POR_DEFECTO)
                .param("ahora", LocalDateTime.now())
                .update();
        return producto;
    }

    @Override
    @Transactional
    public boolean actualizar(Producto cambios) {
        int filas = jdbc.sql("""
                UPDATE producto
                   SET nombre_producto = :nombre, descripcion = :descripcion, precio = :precio,
                       id_categoria = :categoria, imagen = :imagen, emoji = :emoji, badge = :badge,
                       estado_producto = COALESCE(:estado, estado_producto)
                 WHERE id_producto = :id
                """)
                .param("nombre", cambios.nombre_producto)
                .param("descripcion", cambios.descripcion)
                .param("precio", cambios.precio)
                .param("categoria", idCategoria(cambios.categoria))
                .param("imagen", cambios.imagen)
                .param("emoji", cambios.emoji)
                .param("badge", cambios.badge)
                .param("estado", cambios.estado_producto == null || cambios.estado_producto.isBlank()
                        ? null : cambios.estado_producto)
                .param("id", cambios.id_producto)
                .update();
        if (filas == 0) {
            return false;
        }
        jdbc.sql("""
                UPDATE inventario
                   SET cantidad_disponible = :cantidad, fecha_actualizacion = :ahora
                 WHERE id_producto = :id
                """)
                .param("cantidad", Math.max(cambios.stock, 0))
                .param("ahora", LocalDateTime.now())
                .param("id", cambios.id_producto)
                .update();
        return true;
    }

    @Override
    public boolean desactivar(int idProducto) {
        return jdbc.sql("UPDATE producto SET estado_producto = 'inactivo' WHERE id_producto = :id")
                .param("id", idProducto)
                .update() > 0;
    }

    @Override
    public List<Inventario> listarInventario() {
        return jdbc.sql(SELECT_INVENTARIO + " ORDER BY p.nombre_producto")
                .query(ProductoRepositorioJdbc::inventario).list();
    }

    @Override
    public Optional<Inventario> buscarInventario(int idProducto) {
        return jdbc.sql(SELECT_INVENTARIO + " WHERE i.id_producto = :id")
                .param("id", idProducto)
                .query(ProductoRepositorioJdbc::inventario).optional();
    }

    @Override
    @Transactional
    public String actualizarInventario(int idProducto, int cantidad) {
        int filas = jdbc.sql("""
                UPDATE inventario
                   SET cantidad_disponible = :cantidad, fecha_actualizacion = :ahora
                 WHERE id_producto = :id
                """)
                .param("cantidad", Math.max(cantidad, 0))
                .param("ahora", LocalDateTime.now())
                .param("id", idProducto)
                .update();
        if (filas == 0) {
            return "critico";
        }
        return buscarInventario(idProducto).map(Inventario::calcularEstado).orElse("critico");
    }

    @Override
    public boolean descontarInventario(int idProducto, int cantidad) {
        return jdbc.sql("""
                UPDATE inventario
                   SET cantidad_disponible = cantidad_disponible - :cantidad,
                       fecha_actualizacion = :ahora
                 WHERE id_producto = :id AND cantidad_disponible >= :cantidad
                """)
                .param("cantidad", cantidad)
                .param("ahora", LocalDateTime.now())
                .param("id", idProducto)
                .update() == 1;
    }

    private int idCategoria(String nombre) {
        String limpio = nombre == null || nombre.isBlank() ? "Otros" : nombre.trim();
        Optional<Integer> existente = jdbc.sql("SELECT id_categoria FROM categoria WHERE nombre = :nombre")
                .param("nombre", limpio)
                .query(Integer.class).optional();
        if (existente.isPresent()) {
            return existente.get();
        }
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO categoria (nombre) VALUES (:nombre)")
                .param("nombre", limpio)
                .update(llave, "id_categoria");
        return llave.getKey().intValue();
    }

    private static Producto producto(ResultSet rs, int fila) throws SQLException {
        Producto p = new Producto();
        p.id_producto = rs.getInt("id_producto");
        p.nombre_producto = rs.getString("nombre_producto");
        p.descripcion = rs.getString("descripcion");
        p.precio = rs.getBigDecimal("precio");
        p.stock = rs.getInt("stock");
        p.categoria = rs.getString("categoria");
        p.fecha_vencimiento = Filas.fecha(rs, "fecha_vencimiento");
        p.estado_producto = rs.getString("estado_producto");
        p.imagen = rs.getString("imagen");
        p.emoji = rs.getString("emoji");
        p.badge = rs.getString("badge");
        return p;
    }

    private static Inventario inventario(ResultSet rs, int fila) throws SQLException {
        Inventario i = new Inventario();
        i.id_inventario = rs.getInt("id_inventario");
        i.id_producto = rs.getInt("id_producto");
        i.cantidad_disponible = rs.getInt("cantidad_disponible");
        i.cantidad_minima = rs.getInt("cantidad_minima");
        i.fecha_actualizacion = Filas.fechaHora(rs, "fecha_actualizacion");
        i.nombre_producto = rs.getString("nombre_producto");
        i.categoria = rs.getString("categoria");
        i.precio = rs.getBigDecimal("precio");
        i.estado_inventario = i.calcularEstado();
        return i;
    }
}
