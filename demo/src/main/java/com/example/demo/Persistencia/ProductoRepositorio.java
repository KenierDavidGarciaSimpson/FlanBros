package com.example.demo.Persistencia;

import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.Inventario;
import com.example.demo.Modelo.Producto;

public interface ProductoRepositorio {

    List<Producto> listarDisponibles();

    List<Producto> listarTodos();

    List<Producto> listarPorCategoria(String categoria);

    Optional<Producto> buscarPorId(int idProducto);

    Producto crear(Producto producto);

    boolean actualizar(Producto producto);

    boolean desactivar(int idProducto);

    List<Inventario> listarInventario();

    Optional<Inventario> buscarInventario(int idProducto);

    String actualizarInventario(int idProducto, int cantidad);

    boolean descontarInventario(int idProducto, int cantidad);
}
