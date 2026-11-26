package com.example.demo.Persistencia.memoria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.DetallePedido;
import com.example.demo.Modelo.Domicilio;
import com.example.demo.Modelo.Factura;
import com.example.demo.Modelo.Pago;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.Producto;
import com.example.demo.Modelo.dto.ItemPedido;
import com.example.demo.Modelo.dto.NuevoPedido;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Persistencia.PersonaRepositorio;
import com.example.demo.Persistencia.ProductoRepositorio;
import com.example.demo.Persistencia.StockInsuficienteException;

@Profile("memoria")
@Repository
public class PedidoRepositorioMemoria implements PedidoRepositorio {

    private static final BigDecimal TASA_IVA = new BigDecimal("0.08");

    private static final BigDecimal COSTO_DOMICILIO = new BigDecimal("7000");

    private static final List<String> METODOS_PAGO = List.of(
            "contraentrega", "tarjeta", "transferencia", "billetera_digital");

    private static final List<String> ESTADOS_PEDIDO = List.of(
            "pendiente", "en_preparacion", "listo", "entregado", "cancelado");

    private static final List<String> ESTADOS_PAGO = List.of(
            "pendiente", "aprobado", "rechazado");

    private final ProductoRepositorio productos;
    private final PersonaRepositorio personas;

    private final List<Pedido> pedidos;
    private final List<DetallePedido> detalles;
    private final List<Factura> facturas;
    private final List<Pago> pagos;
    private final List<Domicilio> domicilios;

    private int siguientePedidoId;
    private int siguienteDetalleId;
    private int siguienteFacturaId;
    private int siguientePagoId;
    private int siguienteDomicilioId;

    public PedidoRepositorioMemoria(LectorJson lector,
                                    ProductoRepositorio productos,
                                    PersonaRepositorio personas) {
        this.productos = productos;
        this.personas = personas;

        this.pedidos = new ArrayList<>(lector.leerLista("datos/pedidos.json", Pedido.class));
        this.detalles = new ArrayList<>(lector.leerLista("datos/detalles-pedido.json", DetallePedido.class));
        this.facturas = new ArrayList<>(lector.leerLista("datos/facturas.json", Factura.class));
        this.pagos = new ArrayList<>(lector.leerLista("datos/pagos.json", Pago.class));
        this.domicilios = new ArrayList<>(lector.leerLista("datos/domicilios.json", Domicilio.class));

        siguientePedidoId = pedidos.stream().mapToInt(p -> p.id_pedido).max().orElse(0) + 1;
        siguienteDetalleId = detalles.stream().mapToInt(d -> d.id_detalle).max().orElse(0) + 1;
        siguienteFacturaId = facturas.stream().mapToInt(f -> f.id_factura).max().orElse(0) + 1;
        siguientePagoId = pagos.stream().mapToInt(p -> p.id_pago).max().orElse(0) + 1;
        siguienteDomicilioId = domicilios.stream().mapToInt(d -> d.id_domicilio).max().orElse(0) + 1;
    }

    @Override
    public synchronized Pedido crear(NuevoPedido solicitud) {
        List<Producto> productosPedidos = new ArrayList<>();
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
            int disponible = productos.buscarInventario(item.id_producto)
                    .map(fila -> fila.cantidad_disponible)
                    .orElse(producto.stock);
            if (disponible < item.cantidad) {
                throw StockInsuficienteException.para(item.id_producto, disponible, item.cantidad);
            }
            productosPedidos.add(producto);
        }

        Pedido pedido = new Pedido();
        pedido.id_pedido = siguientePedidoId++;
        pedido.fecha_pedido = LocalDateTime.now();
        pedido.estado_pedido = "pendiente";
        pedido.id_cliente = solicitud.id_cliente;
        pedido.observaciones = solicitud.observaciones;

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < solicitud.items.size(); i++) {
            ItemPedido item = solicitud.items.get(i);
            Producto producto = productosPedidos.get(i);

            DetallePedido linea = new DetallePedido();
            linea.id_detalle = siguienteDetalleId++;
            linea.id_pedido = pedido.id_pedido;
            linea.id_producto = producto.id_producto;
            linea.cantidad = item.cantidad;
            linea.precio_unitario = producto.precio;
            linea.subtotal = linea.calcularSubtotal();
            linea.nombre_producto = producto.nombre_producto;

            detalles.add(linea);
            pedido.detalles.add(linea);
            total = total.add(linea.subtotal);

            productos.descontarInventario(producto.id_producto, item.cantidad);
        }

        pedido.total_pedido = total;
        pedidos.add(pedido);

        BigDecimal impuestos = total.multiply(TASA_IVA).setScale(0, RoundingMode.HALF_UP);

        Factura factura = new Factura();
        factura.id_factura = siguienteFacturaId++;
        factura.numero_factura = numeroFactura(pedido.id_pedido);
        factura.fecha_factura = LocalDateTime.now();
        factura.subtotal = total;
        factura.impuestos = impuestos;
        factura.total_factura = total.add(impuestos);
        factura.estado_factura = "pendiente";
        factura.id_cliente = pedido.id_cliente;
        factura.id_pedido = pedido.id_pedido;
        facturas.add(factura);

        Pago pago = new Pago();
        pago.id_pago = siguientePagoId++;
        pago.fecha_pago = LocalDateTime.now();
        pago.monto_pago = factura.total_factura;
        pago.metodo_pago = METODOS_PAGO.contains(solicitud.metodo_pago)
                ? solicitud.metodo_pago : "contraentrega";
        pago.estado_pago = "pendiente";
        pago.id_factura = factura.id_factura;
        pagos.add(pago);

        Domicilio domicilio = new Domicilio();
        domicilio.id_domicilio = siguienteDomicilioId++;
        domicilio.direccion_entrega = solicitud.direccion_entrega;
        domicilio.estado_entrega = "pendiente";
        domicilio.costo_domicilio = COSTO_DOMICILIO;
        domicilio.id_pedido = pedido.id_pedido;
        domicilios.add(domicilio);

        pedido.numero_factura = factura.numero_factura;
        pedido.total_factura = factura.total_factura;
        pedido.estado_factura = factura.estado_factura;
        completarDatosCliente(pedido);
        return pedido;
    }

    @Override
    public synchronized boolean cambiarEstado(int idPedido, String estado) {
        if (estado == null || !ESTADOS_PEDIDO.contains(estado.trim())) {
            return false;
        }
        String nuevoEstado = estado.trim();
        Optional<Pedido> encontrado = pedidos.stream()
                .filter(pedido -> pedido.id_pedido == idPedido)
                .findFirst();
        if (encontrado.isEmpty()) {
            return false;
        }
        encontrado.get().estado_pedido = nuevoEstado;

        switch (nuevoEstado) {
            case "listo" -> cambiarEstadoDomicilioDe(idPedido, "en_camino");
            case "entregado" -> {
                cambiarEstadoDomicilioDe(idPedido, "entregado");
                facturas.stream()
                        .filter(factura -> factura.id_pedido == idPedido)
                        .forEach(factura -> {
                            factura.estado_factura = "pagada";
                            pagos.stream()
                                    .filter(pago -> pago.id_factura == factura.id_factura)
                                    .forEach(pago -> pago.estado_pago = "aprobado");
                        });
            }
            case "cancelado" -> cambiarEstadoDomicilioDe(idPedido, "fallido");
            default -> {
            }
        }
        return true;
    }

    private void cambiarEstadoDomicilioDe(int idPedido, String estado) {
        domicilios.stream()
                .filter(domicilio -> domicilio.id_pedido == idPedido)
                .forEach(domicilio -> domicilio.estado_entrega = estado);
    }

    @Override
    public synchronized List<Pedido> listarRecientes(int limite) {
        int tope = limite <= 0 ? pedidos.size() : limite;
        return pedidos.stream()
                .sorted(POR_FECHA_DESC)
                .limit(tope)
                .map(this::completarPedido)
                .toList();
    }

    @Override
    public synchronized List<Pedido> listarPorCliente(int idCliente) {
        return pedidos.stream()
                .filter(pedido -> pedido.id_cliente == idCliente)
                .sorted(POR_FECHA_DESC)
                .map(this::completarPedido)
                .toList();
    }

    @Override
    public synchronized Optional<Pedido> buscarPorId(int idPedido) {
        return pedidos.stream()
                .filter(pedido -> pedido.id_pedido == idPedido)
                .findFirst()
                .map(this::completarPedido);
    }

    @Override
    public synchronized List<DetallePedido> listarDetalles() {
        completarNombresDeProducto(detalles);
        return List.copyOf(detalles);
    }

    @Override
    public synchronized List<DetallePedido> detallesDe(int idPedido) {
        List<DetallePedido> lineas = detalles.stream()
                .filter(linea -> linea.id_pedido == idPedido)
                .toList();
        completarNombresDeProducto(lineas);
        return lineas;
    }

    @Override
    public synchronized List<Factura> listarFacturas() {
        return facturas.stream()
                .sorted(Comparator.comparing(
                        (Factura factura) -> factura.fecha_factura == null
                                ? LocalDateTime.MIN : factura.fecha_factura).reversed())
                .map(this::completarFactura)
                .toList();
    }

    @Override
    public synchronized List<Factura> listarFacturasPorCliente(int idCliente) {
        return facturas.stream()
                .filter(factura -> factura.id_cliente == idCliente)
                .sorted(POR_FECHA_FACTURA_DESC)
                .map(this::completarFactura)
                .toList();
    }

    @Override
    public synchronized List<Pago> listarPagos() {
        return pagos.stream()
                .sorted(Comparator.comparing(
                        (Pago pago) -> pago.fecha_pago == null
                                ? LocalDateTime.MIN : pago.fecha_pago).reversed())
                .map(this::completarPago)
                .toList();
    }

    @Override
    public synchronized boolean cambiarEstadoPago(int idPago, String estado) {
        if (estado == null || !ESTADOS_PAGO.contains(estado.trim())) {
            return false;
        }
        String nuevoEstado = estado.trim();
        return pagos.stream()
                .filter(pago -> pago.id_pago == idPago)
                .findFirst()
                .map(pago -> {
                    pago.estado_pago = nuevoEstado;
                    return true;
                })
                .orElse(false);
    }

    @Override
    public synchronized List<Domicilio> listarDomicilios() {
        return domicilios.stream()
                .sorted(Comparator.comparing(domicilio -> domicilio.id_domicilio))
                .map(this::completarDomicilio)
                .toList();
    }

    @Override
    public synchronized List<Domicilio> listarDomiciliosPorDomiciliario(int idEmpleado) {
        return domicilios.stream()
                .filter(domicilio -> domicilio.id_domiciliario != null
                        && domicilio.id_domiciliario == idEmpleado)
                .filter(domicilio -> "pendiente".equals(domicilio.estado_entrega)
                        || "en_camino".equals(domicilio.estado_entrega))
                .sorted(Comparator.comparing(domicilio -> domicilio.id_domicilio))
                .map(this::completarDomicilio)
                .toList();
    }

    @Override
    public synchronized Optional<Domicilio> buscarDomicilioPorPedido(int idPedido) {
        return domicilios.stream()
                .filter(domicilio -> domicilio.id_pedido == idPedido)
                .findFirst()
                .map(this::completarDomicilio);
    }

    @Override
    public synchronized boolean actualizarDomicilio(int idDomicilio, String estado, Integer idDomiciliario) {
        Optional<Domicilio> encontrado = domicilios.stream()
                .filter(domicilio -> domicilio.id_domicilio == idDomicilio)
                .findFirst();
        if (encontrado.isEmpty()) {
            return false;
        }
        Domicilio domicilio = encontrado.get();
        if (estado != null && !estado.isBlank()) {
            domicilio.estado_entrega = estado.trim();
        }
        if (idDomiciliario != null) {
            domicilio.id_domiciliario = idDomiciliario;
        }
        return true;
    }

    private static final Comparator<Pedido> POR_FECHA_DESC = Comparator.comparing(
            (Pedido pedido) -> pedido.fecha_pedido == null
                    ? LocalDateTime.MIN : pedido.fecha_pedido).reversed();

    private static final Comparator<Factura> POR_FECHA_FACTURA_DESC = Comparator.comparing(
            (Factura factura) -> factura.fecha_factura == null
                    ? LocalDateTime.MIN : factura.fecha_factura).reversed();

    private String numeroFactura(int idPedido) {
        return "FAC-" + Year.now().getValue() + "-" + String.format("%04d", idPedido);
    }

    private void completarDatosCliente(Pedido pedido) {
        Cliente cliente = personas.buscarCliente(pedido.id_cliente).orElse(null);
        if (cliente != null) {
            pedido.nombre = cliente.nombre;
            pedido.apellido = cliente.apellido;
            pedido.correo = cliente.correo;
        }
    }

    private Pedido completarPedido(Pedido pedido) {
        completarDatosCliente(pedido);
        facturas.stream()
                .filter(factura -> factura.id_pedido == pedido.id_pedido)
                .findFirst()
                .ifPresent(factura -> {
                    pedido.numero_factura = factura.numero_factura;
                    pedido.total_factura = factura.total_factura;
                    pedido.estado_factura = factura.estado_factura;
                });
        pedido.detalles = detallesDe(pedido.id_pedido);
        return pedido;
    }

    private Factura completarFactura(Factura factura) {
        pedidos.stream()
                .filter(pedido -> pedido.id_pedido == factura.id_pedido)
                .findFirst()
                .ifPresent(pedido -> factura.estado_pedido = pedido.estado_pedido);
        personas.buscarCliente(factura.id_cliente).ifPresent(cliente -> {
            factura.nombre = cliente.nombre;
            factura.apellido = cliente.apellido;
            factura.correo = cliente.correo;
        });
        return factura;
    }

    private Pago completarPago(Pago pago) {
        facturas.stream()
                .filter(factura -> factura.id_factura == pago.id_factura)
                .findFirst()
                .ifPresent(factura -> {
                    pago.numero_factura = factura.numero_factura;
                    pago.total_factura = factura.total_factura;
                    personas.buscarCliente(factura.id_cliente).ifPresent(cliente -> {
                        pago.nombre = cliente.nombre;
                        pago.apellido = cliente.apellido;
                        pago.correo = cliente.correo;
                    });
                });
        return pago;
    }

    private Domicilio completarDomicilio(Domicilio domicilio) {
        pedidos.stream()
                .filter(pedido -> pedido.id_pedido == domicilio.id_pedido)
                .findFirst()
                .ifPresent(pedido -> {
                    domicilio.estado_pedido = pedido.estado_pedido;
                    domicilio.total_pedido = pedido.total_pedido;
                    personas.buscarCliente(pedido.id_cliente).ifPresent(cliente -> {
                        domicilio.nombre = cliente.nombre;
                        domicilio.apellido = cliente.apellido;
                        domicilio.telefono = cliente.telefono;
                    });
                });
        facturas.stream()
                .filter(factura -> factura.id_pedido == domicilio.id_pedido)
                .findFirst()
                .ifPresent(factura -> domicilio.numero_factura = factura.numero_factura);
        if (domicilio.id_domiciliario != null) {
            personas.buscarEmpleado(domicilio.id_domiciliario)
                    .ifPresent(empleado -> domicilio.nombre_domiciliario = empleado.nombre);
        }
        return domicilio;
    }

    private void completarNombresDeProducto(List<DetallePedido> lineas) {
        lineas.forEach(linea -> {
            if (linea.nombre_producto == null) {
                productos.buscarPorId(linea.id_producto)
                        .ifPresent(producto -> linea.nombre_producto = producto.nombre_producto);
            }
        });
    }
}
