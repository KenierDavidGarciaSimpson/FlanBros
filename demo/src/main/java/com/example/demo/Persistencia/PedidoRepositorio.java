package com.example.demo.Persistencia;

import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.DetallePedido;
import com.example.demo.Modelo.Domicilio;
import com.example.demo.Modelo.Factura;
import com.example.demo.Modelo.Pago;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.dto.NuevoPedido;

public interface PedidoRepositorio {

    List<Pedido> listarRecientes(int limite);

    List<Pedido> listarPorCliente(int idCliente);

    Optional<Pedido> buscarPorId(int idPedido);

    List<DetallePedido> listarDetalles();

    List<DetallePedido> detallesDe(int idPedido);

    Pedido crear(NuevoPedido solicitud);

    boolean cambiarEstado(int idPedido, String estado);

    List<Factura> listarFacturas();

    List<Factura> listarFacturasPorCliente(int idCliente);

    List<Pago> listarPagos();

    boolean cambiarEstadoPago(int idPago, String estado);

    List<Domicilio> listarDomicilios();

    List<Domicilio> listarDomiciliosPorDomiciliario(int idEmpleado);

    Optional<Domicilio> buscarDomicilioPorPedido(int idPedido);

    boolean actualizarDomicilio(int idDomicilio, String estado, Integer idDomiciliario);
}
