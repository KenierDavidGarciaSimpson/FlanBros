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

@RestController
@RequestMapping("/api/domicilios")
public class DomiciliosApiControlador {

    private static final List<String> ESTADOS = List.of("pendiente", "en_camino", "entregado", "fallido");

    private final PedidoRepositorio pedidos;

    public DomiciliosApiControlador(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    public RespuestaApi listar(@RequestParam(name = "id_domiciliario", required = false) Integer idDomiciliario) {
        Optional<Integer> propio = UsuarioActual.domiciliarioPropio();
        if (propio.isPresent()) {
            return RespuestaApi.ok(pedidos.listarDomiciliosPorDomiciliario(propio.get()));
        }
        if (idDomiciliario != null) {
            return RespuestaApi.ok(pedidos.listarDomiciliosPorDomiciliario(idDomiciliario));
        }
        return RespuestaApi.ok(pedidos.listarDomicilios());
    }

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
