package com.example.demo.Persistencia.memoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.Inventario;
import com.example.demo.Modelo.Producto;
import com.example.demo.Persistencia.ProductoRepositorio;

/**
 * ProductoRepositorioMemoria — capa 2 (Persistencia), implementacion en memoria.
 *
 * <p>Carga {@code datos/productos.json} y {@code datos/inventario.json} una sola vez
 * al arrancar y desde ahi trabaja con listas en memoria (los cambios se pierden al
 * reiniciar). Es el sustituto de MySQL mientras no hay base de datos.</p>
 *
 * <p>Dos detalles que se conservan del comportamiento original:</p>
 * <ul>
 *   <li>El stock se escribe en los dos sitios ({@code Producto.stock} e
 *       {@code Inventario.cantidad_disponible}), como hacia {@code inventario.php}.</li>
 *   <li>Borrar un producto es desactivarlo, nunca quitarlo: los pedidos viejos lo
 *       siguen nombrando.</li>
 * </ul>
 *
 * <p>Correccion respecto al PHP: al crear un producto tambien se crea su fila de
 * inventario. Antes el INSERT solo tocaba {@code Producto} y luego el checkout
 * rechazaba ese producto con "Stock insuficiente" porque no encontraba su fila en
 * {@code Inventario}.</p>
 *
 * <p>Capa: Persistencia. Implementa {@link ProductoRepositorio}.</p>
 */
@Profile("memoria")
@Repository
public class ProductoRepositorioMemoria implements ProductoRepositorio {

    private static final int STOCK_MINIMO_POR_DEFECTO = 5;
    private static final String DISPONIBLE = "disponible";
    private static final String INACTIVO = "inactivo";

    private static final Comparator<Producto> POR_NOMBRE =
            Comparator.comparing(producto -> producto.nombre_producto,
                    String.CASE_INSENSITIVE_ORDER);

    private final List<Producto> productos = new ArrayList<>();
    private final List<Inventario> inventario = new ArrayList<>();

    private int siguienteProductoId;
    private int siguienteInventarioId;

    public ProductoRepositorioMemoria(LectorJson lector) {
        productos.addAll(lector.leerLista("datos/productos.json", Producto.class));
        inventario.addAll(lector.leerLista("datos/inventario.json", Inventario.class));

        siguienteProductoId = productos.stream()
                .mapToInt(producto -> producto.id_producto).max().orElse(0) + 1;
        siguienteInventarioId = inventario.stream()
                .mapToInt(fila -> fila.id_inventario).max().orElse(0) + 1;

        inventario.forEach(this::completarConProducto);
    }

    @Override
    public synchronized List<Producto> listarDisponibles() {
        return productos.stream()
                .filter(Producto::estaDisponible)
                .sorted(POR_NOMBRE)
                .toList();
    }

    @Override
    public synchronized List<Producto> listarTodos() {
        return productos.stream().sorted(POR_NOMBRE).toList();
    }

    @Override
    public synchronized List<Producto> listarPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return listarDisponibles();
        }
        String buscada = categoria.trim();
        return productos.stream()
                .filter(Producto::estaDisponible)
                .filter(producto -> buscada.equalsIgnoreCase(producto.categoria))
                .sorted(POR_NOMBRE)
                .toList();
    }

    @Override
    public synchronized Optional<Producto> buscarPorId(int idProducto) {
        return productos.stream()
                .filter(producto -> producto.id_producto == idProducto)
                .findFirst();
    }

    @Override
    public synchronized Producto crear(Producto producto) {
        producto.id_producto = siguienteProductoId++;
        if (producto.estado_producto == null || producto.estado_producto.isBlank()) {
            producto.estado_producto = DISPONIBLE;
        }
        productos.add(producto);

        // Sin esta fila el producto se podria vender pero el checkout lo rechazaria.
        Inventario fila = new Inventario();
        fila.id_inventario = siguienteInventarioId++;
        fila.id_producto = producto.id_producto;
        fila.cantidad_disponible = Math.max(producto.stock, 0);
        fila.cantidad_minima = STOCK_MINIMO_POR_DEFECTO;
        fila.fecha_actualizacion = LocalDateTime.now();
        fila.estado_inventario = fila.calcularEstado();
        inventario.add(fila);
        completarConProducto(fila);

        return producto;
    }

    @Override
    public synchronized boolean actualizar(Producto cambios) {
        Optional<Producto> encontrado = buscarPorId(cambios.id_producto);
        if (encontrado.isEmpty()) {
            return false;
        }
        Producto producto = encontrado.get();
        producto.nombre_producto = cambios.nombre_producto;
        producto.descripcion = cambios.descripcion;
        producto.precio = cambios.precio;
        producto.stock = cambios.stock;
        producto.categoria = cambios.categoria;
        producto.imagen = cambios.imagen;
        producto.emoji = cambios.emoji;
        producto.badge = cambios.badge;
        if (cambios.estado_producto != null && !cambios.estado_producto.isBlank()) {
            producto.estado_producto = cambios.estado_producto;
        }

        // El stock se mantiene sincronizado con el inventario, como en el PHP.
        buscarInventario(cambios.id_producto).ifPresent(fila -> {
            fila.cantidad_disponible = Math.max(producto.stock, 0);
            fila.estado_inventario = fila.calcularEstado();
            fila.fecha_actualizacion = LocalDateTime.now();
        });
        return true;
    }

    @Override
    public synchronized boolean desactivar(int idProducto) {
        return buscarPorId(idProducto)
                .map(producto -> {
                    producto.estado_producto = INACTIVO;
                    return true;
                })
                .orElse(false);
    }

    // ------------------------------------------------------------------
    // Inventario
    // ------------------------------------------------------------------

    @Override
    public synchronized List<Inventario> listarInventario() {
        inventario.forEach(this::completarConProducto);
        return inventario.stream()
                .sorted(Comparator.comparing(
                        fila -> fila.nombre_producto == null ? "" : fila.nombre_producto,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public synchronized Optional<Inventario> buscarInventario(int idProducto) {
        return inventario.stream()
                .filter(fila -> fila.id_producto == idProducto)
                .findFirst();
    }

    @Override
    public synchronized String actualizarInventario(int idProducto, int cantidad) {
        Optional<Inventario> encontrado = buscarInventario(idProducto);
        if (encontrado.isEmpty()) {
            return "critico";
        }
        Inventario fila = encontrado.get();
        fila.cantidad_disponible = Math.max(cantidad, 0);
        fila.estado_inventario = fila.calcularEstado();
        fila.fecha_actualizacion = LocalDateTime.now();

        buscarPorId(idProducto).ifPresent(producto -> producto.stock = fila.cantidad_disponible);
        return fila.estado_inventario;
    }

    @Override
    public synchronized boolean descontarInventario(int idProducto, int cantidad) {
        Optional<Inventario> encontrado = buscarInventario(idProducto);
        if (encontrado.isEmpty()) {
            return false;
        }
        Inventario fila = encontrado.get();
        if (fila.cantidad_disponible < cantidad) {
            return false;
        }
        fila.cantidad_disponible -= cantidad;
        fila.estado_inventario = fila.calcularEstado();
        fila.fecha_actualizacion = LocalDateTime.now();

        buscarPorId(idProducto).ifPresent(producto -> producto.stock = fila.cantidad_disponible);
        return true;
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /** Rellena los campos que en SQL traia el JOIN con Producto. */
    private void completarConProducto(Inventario fila) {
        productos.stream()
                .filter(producto -> producto.id_producto == fila.id_producto)
                .findFirst()
                .ifPresent(producto -> {
                    fila.nombre_producto = producto.nombre_producto;
                    fila.categoria = producto.categoria;
                    fila.precio = producto.precio;
                    fila.estado_inventario = fila.calcularEstado();
                });
    }
}
