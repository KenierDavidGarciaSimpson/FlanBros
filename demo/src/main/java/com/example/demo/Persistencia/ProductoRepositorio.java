package com.example.demo.Persistencia;

import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.Inventario;
import com.example.demo.Modelo.Producto;

/**
 * ProductoRepositorio — capa 2 (Persistencia), INTERFAZ.
 *
 * <p>Es el unico contrato que conoce el controlador. Hoy lo implementa
 * {@code memoria/ProductoRepositorioMemoria} (listas en memoria cargadas de un JSON);
 * el dia que se conecte PostgreSQL se escribe {@code jpa/ProductoRepositorioJpa} y no se
 * toca ni una linea del controlador.</p>
 *
 * <p>Incluye el inventario a proposito: en el diseno original el stock vive en
 * {@code Producto.stock} y en {@code Inventario.cantidad_disponible}, y se mueven
 * juntos. Separarlos en dos repositorios obligaria a que ambos se conocieran.</p>
 *
 * <p>Nada de SQL aqui: esta capa solo dice QUE se necesita, no COMO se obtiene.</p>
 */
public interface ProductoRepositorio {

    /** Productos con estado "disponible", ordenados por nombre (lo que ve el catalogo). */
    List<Producto> listarDisponibles();

    /** Todos, incluidos los inactivos (lo que ve el panel de administracion). */
    List<Producto> listarTodos();

    /** Disponibles de una categoria concreta. */
    List<Producto> listarPorCategoria(String categoria);

    Optional<Producto> buscarPorId(int idProducto);

    /** Crea el producto, le asigna el siguiente id y le abre su fila de inventario. */
    Producto crear(Producto producto);

    /** Actualiza los campos editables. false si el id no existe. */
    boolean actualizar(Producto producto);

    /** Baja logica: estado_producto = "inactivo" (el historial de pedidos se conserva). */
    boolean desactivar(int idProducto);

    /** Inventario con los datos del producto ya cruzados, para la tabla del panel. */
    List<Inventario> listarInventario();

    Optional<Inventario> buscarInventario(int idProducto);

    /**
     * Fija la cantidad disponible, recalcula el estado y sincroniza
     * {@code Producto.stock}. Devuelve el estado nuevo ("normal", "bajo", "critico").
     */
    String actualizarInventario(int idProducto, int cantidad);

    /**
     * Resta existencias al confirmar un pedido. false si no hay suficientes
     * (el que llama debe abortar antes de crear nada).
     */
    boolean descontarInventario(int idProducto, int cantidad);
}
