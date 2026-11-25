package com.example.demo.Controlador.api;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Modelo.Inventario;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Persistencia.PersonaRepositorio;
import com.example.demo.Persistencia.ProductoRepositorio;

/**
 * DashboardApiControlador — capa 3 (Controlador), API de metricas.
 *
 * <p>Sustituye a {@code backend/api/dashboard.php}, que resolvia la pantalla con
 * seis consultas de agregacion (SUM, COUNT, GROUP BY). Aqui se calcula sobre las
 * listas que ya entrega la capa 2: es el mismo resultado con menos piezas.</p>
 *
 * <p>Este controlador cruza los tres repositorios a proposito: un tablero de
 * indicadores es, por definicion, una vista que suma todo. Ninguna de esas sumas se
 * guarda: se recalculan en cada peticion para que nunca queden desfasadas.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiControlador {

    private static final int PEDIDOS_RECIENTES = 5;

    private final PedidoRepositorio pedidos;
    private final ProductoRepositorio productos;
    private final PersonaRepositorio personas;

    public DashboardApiControlador(PedidoRepositorio pedidos,
                                   ProductoRepositorio productos,
                                   PersonaRepositorio personas) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.personas = personas;
    }

    /**
     * GET /api/dashboard — los indicadores de la pantalla de inicio del panel.
     *
     * <p>Las claves son las que lee {@code admin-dashboard.js}
     * ({@code ventas_mes}, {@code pedidos_mes}, {@code clientes_activos},
     * {@code domicilios_activos}, {@code ultimos_pedidos}, {@code stock_critico}); cambiar
     * un nombre aqui deja la tarjeta en blanco sin error visible.</p>
     */
    @GetMapping
    public RespuestaApi metricas() {
        YearMonth mesActual = YearMonth.now();
        List<Pedido> todos = pedidos.listarRecientes(0);

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("ventas_mes", ventasDelMes(mesActual));
        metricas.put("pedidos_mes", pedidosDelMes(todos, mesActual));
        metricas.put("clientes_activos", clientesActivos());
        metricas.put("domicilios_activos", domiciliosActivos());
        metricas.put("ultimos_pedidos", todos.stream().limit(PEDIDOS_RECIENTES).toList());
        metricas.put("stock_critico", stockCritico());

        return RespuestaApi.ok(metricas);
    }

    /**
     * Total facturado en el mes en curso. Se cuentan solo las facturas vigentes: una
     * anulada no es venta, aunque su pedido exista.
     */
    private BigDecimal ventasDelMes(YearMonth mes) {
        return pedidos.listarFacturas().stream()
                .filter(factura -> factura.fecha_factura != null
                        && YearMonth.from(factura.fecha_factura).equals(mes))
                .filter(factura -> !"anulada".equals(factura.estado_factura))
                .map(factura -> factura.total_factura == null ? BigDecimal.ZERO : factura.total_factura)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long pedidosDelMes(List<Pedido> todos, YearMonth mes) {
        return todos.stream()
                .filter(pedido -> pedido.fecha_pedido != null
                        && YearMonth.from(pedido.fecha_pedido).equals(mes))
                .count();
    }

    /** Clientes con la cuenta abierta; los que tienen el estado en null cuentan como activos. */
    private long clientesActivos() {
        return personas.listarClientes().stream()
                .filter(cliente -> !"inactivo".equals(cliente.estado_cliente))
                .count();
    }

    /** Envios que todavia no han llegado al cliente (pendientes o en camino). */
    private long domiciliosActivos() {
        return pedidos.listarDomicilios().stream()
                .filter(domicilio -> "pendiente".equals(domicilio.estado_entrega)
                        || "en_camino".equals(domicilio.estado_entrega))
                .count();
    }

    /** Filas de inventario por debajo de su minimo (la lista "stock critico" del panel). */
    private List<Inventario> stockCritico() {
        return productos.listarInventario().stream()
                .filter(inventario -> !"normal".equals(inventario.estado_inventario))
                .toList();
    }
}
