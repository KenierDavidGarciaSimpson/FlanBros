package com.example.demo.Controlador.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Controlador.util.ValidacionUtil;
import com.example.demo.Modelo.Cupon;
import com.example.demo.Modelo.dto.ItemPedido;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.dto.NuevoPedido;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.CuponRepositorio;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Persistencia.StockInsuficienteException;
import com.example.demo.Seguridad.UsuarioActual;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosApiControlador {

    private static final List<String> METODOS_PAGO = List.of(
            "contraentrega", "tarjeta", "transferencia", "billetera_digital");
    private static final int MAX_LINEAS = 30;
    private static final int MAX_UNIDADES_POR_LINEA = 50;

    private final PedidoRepositorio pedidos;
    private final CuponRepositorio cupones;

    public PedidosApiControlador(PedidoRepositorio pedidos, CuponRepositorio cupones) {
        this.pedidos = pedidos;
        this.cupones = cupones;
    }

    @GetMapping
    public RespuestaApi listar(@RequestParam(required = false) Integer id,
                               @RequestParam(name = "id_cliente", required = false) Integer idCliente) {
        Optional<Integer> propio = UsuarioActual.clientePropio();
        if (propio.isPresent()) {
            int idPropio = propio.get();
            if (id != null) {
                return pedidos.buscarPorId(id)
                        .filter(pedido -> pedido.id_cliente == idPropio)
                        .<RespuestaApi>map(RespuestaApi::ok)
                        .orElseGet(() -> RespuestaApi.error("Pedido no encontrado"));
            }
            return RespuestaApi.ok(pedidos.listarPorCliente(idPropio));
        }
        if (id != null) {
            return pedidos.buscarPorId(id)
                    .<RespuestaApi>map(RespuestaApi::ok)
                    .orElseGet(() -> RespuestaApi.error("Pedido no encontrado"));
        }
        if (idCliente != null) {
            return RespuestaApi.ok(pedidos.listarPorCliente(idCliente));
        }
        return RespuestaApi.ok(pedidos.listarRecientes(0));
    }

    @PostMapping
    public ResponseEntity<RespuestaApi> crear(@RequestBody NuevoPedido solicitud) {
        UsuarioActual.clientePropio().ifPresent(idPropio -> solicitud.id_cliente = idPropio);
        if (!solicitud.estaCompleto()) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "Faltan datos obligatorios: id_cliente, items y direccion_entrega"));
        }
        String error = validar(solicitud);
        if (error != null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(error));
        }
        try {
            Pedido pedido = pedidos.crear(solicitud);

            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("id_pedido", pedido.id_pedido);
            datos.put("numero_factura", pedido.numero_factura);
            datos.put("estado_pedido", pedido.estado_pedido);
            datos.put("subtotal", pedido.total_pedido);
            datos.put("total", pedido.total_factura);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(RespuestaApi.ok(datos, "Pedido creado correctamente"));
        } catch (StockInsuficienteException e) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(e.getMessage()));
        }
    }

    private String validar(NuevoPedido solicitud) {
        solicitud.direccion_entrega = solicitud.direccion_entrega.trim();
        if (solicitud.observaciones != null) {
            solicitud.observaciones = solicitud.observaciones.trim();
            if (solicitud.observaciones.isEmpty()) {
                solicitud.observaciones = null;
            }
        }
        String error = ValidacionUtil.primero(
                ValidacionUtil.largoEntre("dirección de entrega", solicitud.direccion_entrega, 5, 200),
                ValidacionUtil.largoMaximo("observaciones", solicitud.observaciones, 300));
        if (error != null) {
            return error;
        }
        if (solicitud.metodo_pago == null || !METODOS_PAGO.contains(solicitud.metodo_pago)) {
            return "Elige un método de pago válido";
        }
        if (solicitud.items.size() > MAX_LINEAS) {
            return "El pedido no puede tener más de " + MAX_LINEAS + " productos distintos";
        }
        for (ItemPedido item : solicitud.items) {
            if (item == null || !item.esValido()) {
                return "Hay un producto con cantidad inválida en el pedido";
            }
            if (item.cantidad > MAX_UNIDADES_POR_LINEA) {
                return "No puedes pedir más de " + MAX_UNIDADES_POR_LINEA + " unidades de un mismo producto";
            }
        }
        String codigo = solicitud.codigo_cupon == null ? null : solicitud.codigo_cupon.trim();
        if (codigo != null && !codigo.isEmpty()) {
            Optional<Cupon> cupon = cupones.buscarPorCodigo(codigo);
            if (cupon.isEmpty()) {
                return "El cupón " + codigo + " no es válido o está vencido";
            }
            solicitud.descuento_pct = cupon.get().descuento_pct;
            solicitud.cupon_aplicado = cupon.get().codigoNormalizado();
        }
        return null;
    }

    @PutMapping
    public ResponseEntity<RespuestaApi> cambiarEstado(@RequestBody Map<String, Object> cuerpo) {
        int idPedido = PeticionUtil.entero(cuerpo, "id_pedido");
        String estado = PeticionUtil.texto(cuerpo, "estado_pedido", PeticionUtil.texto(cuerpo, "estado"));
        if (estado == null) {
            return ResponseEntity.badRequest().body(RespuestaApi.error("Falta el nuevo estado"));
        }
        if (UsuarioActual.domiciliarioPropio().isPresent()
                && !List.of("listo", "entregado").contains(estado)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(RespuestaApi.error("No tienes permiso para cambiar a ese estado"));
        }
        if (!pedidos.cambiarEstado(idPedido, estado)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("No se pudo cambiar el estado del pedido"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(pedidos.buscarPorId(idPedido).orElse(null),
                "Estado actualizado a " + estado));
    }
}
