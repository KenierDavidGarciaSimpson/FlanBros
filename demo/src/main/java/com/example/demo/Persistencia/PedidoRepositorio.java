package com.example.demo.Persistencia;

import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.DetallePedido;
import com.example.demo.Modelo.Domicilio;
import com.example.demo.Modelo.Factura;
import com.example.demo.Modelo.Pago;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.dto.NuevoPedido;

/**
 * PedidoRepositorio — capa 2 (Persistencia), INTERFAZ.
 *
 * <p>Un pedido no es una fila: al confirmarlo se escriben seis tablas a la vez
 * ({@code Pedido}, {@code Detalle_Pedido}, {@code Factura}, {@code Pago},
 * {@code Domicilio} y el stock). Por eso todo eso vive en un mismo repositorio:
 * {@link #crear(NuevoPedido)} es la antigua transaccion de {@code pedidos.php}.
 * Si algo falla, no se crea nada.</p>
 *
 * <p>Capa: Persistencia.</p>
 */
public interface PedidoRepositorio {

    /** Ultimos pedidos con cliente y factura cruzados (tabla del panel). */
    List<Pedido> listarRecientes(int limite);

    /** Historial de un cliente, con su numero de factura (lo que pinta cuenta.js). */
    List<Pedido> listarPorCliente(int idCliente);

    Optional<Pedido> buscarPorId(int idPedido);

    /** Todas las lineas de todos los pedidos (para el top de vendidos del dashboard). */
    List<DetallePedido> listarDetalles();

    List<DetallePedido> detallesDe(int idPedido);

    /**
     * Crea el pedido completo: valida stock, guarda cabecera y lineas, descuenta
     * inventario, emite la factura (con IVA), registra el pago pendiente y el
     * domicilio. Devuelve el pedido ya con su factura enlazada.
     *
     * @throws StockInsuficienteException si algun producto no alcanza. En ese caso
     *         no queda nada escrito.
     */
    Pedido crear(NuevoPedido solicitud);

    /**
     * Cambia el estado del pedido y aplica los efectos que ya aplicaba el PHP:
     * entregado implica factura pagada, pago aprobado y domicilio entregado;
     * cancelado deja el domicilio fallido; listo pone el domicilio en camino.
     */
    boolean cambiarEstado(int idPedido, String estado);

    List<Factura> listarFacturas();

    List<Factura> listarFacturasPorCliente(int idCliente);

    List<Pago> listarPagos();

    boolean cambiarEstadoPago(int idPago, String estado);

    List<Domicilio> listarDomicilios();

    /** Domicilios pendientes o en camino de un repartidor concreto. */
    List<Domicilio> listarDomiciliosPorDomiciliario(int idEmpleado);

    /**
     * Domicilio de un pedido (cada pedido tiene exactamente uno).
     *
     * <p>Hace falta porque {@code domiciliario.js} identifica el envio por el pedido, no
     * por el id del domicilio: el repartidor pulsa "salir a entregar" sobre una tarjeta
     * que lleva {@code id_pedido}.</p>
     */
    Optional<Domicilio> buscarDomicilioPorPedido(int idPedido);

    boolean actualizarDomicilio(int idDomicilio, String estado, Integer idDomiciliario);
}
