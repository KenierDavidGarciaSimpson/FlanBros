package com.example.demo.Controlador.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Modelo.Domicilio;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Seguridad.UsuarioActual;

/**
 * DomiciliosApiControlador — capa 3 (Controlador), API de envios.
 *
 * <p>Sustituye a {@code backend/api/domicilios.php}. Dos vistas:</p>
 * <ul>
 *   <li>El panel ve todos los envios con cliente, factura y repartidor.</li>
 *   <li>El domiciliario ({@code domiciliario.html}) ve solo los suyos: pendientes y
 *       en camino, filtrados por {@code id_domiciliario}.</li>
 * </ul>
 *
 * <p>Al asignar un repartidor el {@code id_domiciliario} puede llegar en null (para
 * liberar el envio); por eso se lee con {@link PeticionUtil#enteroOpcional}.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/domicilios")
public class DomiciliosApiControlador {

    private static final List<String> ESTADOS = List.of("pendiente", "en_camino", "entregado", "fallido");

    private final PedidoRepositorio pedidos;

    public DomiciliosApiControlador(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    /**
     * GET /api/domicilios                        -> todos (panel)
     * GET /api/domicilios?id_domiciliario=4      -> los de un repartidor
     */
    @GetMapping
    public RespuestaApi listar(@RequestParam(name = "id_domiciliario", required = false) Integer idDomiciliario) {
        // Un repartidor solo ve SUS entregas.
        Optional<Integer> propio = UsuarioActual.domiciliarioPropio();
        if (propio.isPresent()) {
            return RespuestaApi.ok(pedidos.listarDomiciliosPorDomiciliario(propio.get()));
        }
        if (idDomiciliario != null) {
            return RespuestaApi.ok(pedidos.listarDomiciliosPorDomiciliario(idDomiciliario));
        }
        return RespuestaApi.ok(pedidos.listarDomicilios());
    }

    /**
     * PUT /api/domicilios — {id_domicilio, estado_entrega, id_domiciliario}.
     *
     * <p>{@code domiciliario.js} no conoce el id del domicilio cuando el pedido todavia
     * esta "listo": manda {@code id_pedido} y el backend resuelve cual es su envio.</p>
     */
    @PutMapping
    public ResponseEntity<RespuestaApi> actualizar(@RequestBody Map<String, Object> cuerpo) {
        String estado = PeticionUtil.texto(cuerpo, "estado_entrega", PeticionUtil.texto(cuerpo, "estado"));
        Integer idDomiciliario = PeticionUtil.enteroOpcional(cuerpo, "id_domiciliario");

        if (estado == null || !ESTADOS.contains(estado)) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "Estado no valido: usa pendiente, en_camino, entregado o fallido"));
        }

        int idDomicilio = PeticionUtil.entero(cuerpo, "id_domicilio");
        if (idDomicilio <= 0) {
            Integer idPedido = PeticionUtil.enteroOpcional(cuerpo, "id_pedido");
            if (idPedido == null) {
                return ResponseEntity.badRequest().body(RespuestaApi.error(
                        "Falta el domicilio: envia id_domicilio o id_pedido"));
            }
            idDomicilio = pedidos.buscarDomicilioPorPedido(idPedido)
                    .map(domicilio -> domicilio.id_domicilio)
                    .orElse(0);
            if (idDomicilio == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RespuestaApi.error("Ese pedido no tiene domicilio registrado"));
            }
        }

        // Un repartidor no puede tocar entregas de otro ni asignarlas a otra persona.
        Optional<Integer> propio = UsuarioActual.domiciliarioPropio();
        if (propio.isPresent()) {
            final int objetivo = idDomicilio;
            Optional<Domicilio> actual = pedidos.listarDomicilios().stream()
                    .filter(domicilio -> domicilio.id_domicilio == objetivo).findFirst();
            if (actual.isPresent() && actual.get().id_domiciliario != null
                    && !actual.get().id_domiciliario.equals(propio.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(RespuestaApi.error("Ese domicilio esta asignado a otro repartidor"));
            }
            if (idDomiciliario != null) {
                idDomiciliario = propio.get();
            }
        }

        if (!pedidos.actualizarDomicilio(idDomicilio, estado, idDomiciliario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Domicilio no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(null, "Domicilio actualizado"));
    }
}
