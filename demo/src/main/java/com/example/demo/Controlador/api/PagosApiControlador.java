package com.example.demo.Controlador.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.PedidoRepositorio;

/**
 * PagosApiControlador — capa 3 (Controlador), API de pagos.
 *
 * <p>Sustituye a {@code backend/api/pagos.php}. El panel lista los pagos con su
 * factura y su cliente ya cruzados y puede aprobarlos o rechazarlos a mano.
 * Lo normal es que se aprueben solos cuando el pedido se marca entregado
 * (ver {@code PedidoRepositorio.cambiarEstado}).</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/pagos")
public class PagosApiControlador {

    private static final List<String> ESTADOS = List.of("pendiente", "aprobado", "rechazado");

    private final PedidoRepositorio pedidos;

    public PagosApiControlador(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    /** GET /api/pagos — todos los pagos con factura y cliente. */
    @GetMapping
    public RespuestaApi listar() {
        return RespuestaApi.ok(pedidos.listarPagos());
    }

    /** PUT /api/pagos — {id_pago, estado_pago}. */
    @PutMapping
    public ResponseEntity<RespuestaApi> cambiarEstado(@RequestBody Map<String, Object> cuerpo) {
        int idPago = PeticionUtil.entero(cuerpo, "id_pago");
        String estado = PeticionUtil.texto(cuerpo, "estado_pago", PeticionUtil.texto(cuerpo, "estado"));

        if (estado == null || !ESTADOS.contains(estado)) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "Estado no valido: usa pendiente, aprobado o rechazado"));
        }
        if (!pedidos.cambiarEstadoPago(idPago, estado)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Pago no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(null, "Pago " + estado));
    }
}
