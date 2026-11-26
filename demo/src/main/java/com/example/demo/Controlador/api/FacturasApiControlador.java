package com.example.demo.Controlador.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Modelo.DetallePedido;
import com.example.demo.Modelo.Factura;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Seguridad.UsuarioActual;

/**
 * FacturasApiControlador — capa 3 (Controlador), API de facturas.
 *
 * <p>Sustituye a {@code backend/api/facturas.php}. Solo lectura: las facturas se
 * emiten automaticamente al confirmar el pedido, no se crean a mano.
 * Lo consume {@code admin-facturas.js}.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/facturas")
public class FacturasApiControlador {

    private final PedidoRepositorio pedidos;

    public FacturasApiControlador(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    /**
     * GET /api/facturas
     * GET /api/facturas?id_cliente=2
     */
    @GetMapping
    public RespuestaApi listar(@RequestParam(name = "id_cliente", required = false) Integer idCliente) {
        // Un cliente solo ve SUS facturas, sin importar que id mande el navegador.
        if (UsuarioActual.clientePropio().isPresent()) {
            return RespuestaApi.ok(pedidos.listarFacturasPorCliente(UsuarioActual.clientePropio().get()));
        }
        if (idCliente != null) {
            return RespuestaApi.ok(pedidos.listarFacturasPorCliente(idCliente));
        }
        return RespuestaApi.ok(pedidos.listarFacturas());
    }

    /**
     * GET /api/facturas/pedido/{idPedido} — la factura completa de un pedido: productos,
     * descuento del cupon, IVA, total, pago y domicilio. El cliente solo puede ver las
     * suyas; administrador y contador ven cualquiera.
     */
    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<RespuestaApi> detalle(@PathVariable int idPedido) {
        Optional<Integer> clientePropio = UsuarioActual.clientePropio();
        List<Factura> candidatas = clientePropio.isPresent()
                ? pedidos.listarFacturasPorCliente(clientePropio.get())
                : pedidos.listarFacturas();
        Optional<Factura> encontrada = candidatas.stream()
                .filter(f -> f.id_pedido == idPedido)
                .findFirst();
        if (encontrada.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Factura no encontrada"));
        }
        Factura factura = encontrada.get();
        List<DetallePedido> lineas = pedidos.detallesDe(idPedido);

        // factura.subtotal ya trae el descuento del cupon; la diferencia con la suma
        // de las lineas es lo que se desconto.
        BigDecimal sumaLineas = lineas.stream()
                .map(l -> l.subtotal == null ? BigDecimal.ZERO : l.subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuento = sumaLineas.subtract(factura.subtotal).max(BigDecimal.ZERO);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("factura", factura);
        datos.put("lineas", lineas);
        datos.put("subtotal_productos", sumaLineas);
        datos.put("descuento", descuento);
        datos.put("observaciones", pedidos.buscarPorId(idPedido).map(p -> p.observaciones).orElse(null));
        pedidos.listarPagos().stream()
                .filter(g -> g.id_factura == factura.id_factura)
                .findFirst()
                .ifPresent(g -> {
                    datos.put("metodo_pago", g.metodo_pago);
                    datos.put("estado_pago", g.estado_pago);
                });
        pedidos.buscarDomicilioPorPedido(idPedido).ifPresent(d -> {
            datos.put("direccion_entrega", d.direccion_entrega);
            datos.put("costo_domicilio", d.costo_domicilio);
            datos.put("estado_entrega", d.estado_entrega);
        });
        return ResponseEntity.ok(RespuestaApi.ok(datos));
    }
}
