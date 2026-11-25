package com.example.demo.Controlador.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.Cliente;
import com.example.demo.Modelo.Pedido;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.PedidoRepositorio;
import com.example.demo.Persistencia.PersonaRepositorio;

/**
 * ClientesApiControlador — capa 3 (Controlador), API de clientes del panel.
 *
 * <p>Sustituye a {@code backend/api/clientes.php}. El {@code total_pedidos} que
 * pinta {@code admin-clientes.js} era un COUNT con JOIN en SQL; aqui se cuenta con
 * los pedidos que entrega la capa 2, en una sola pasada.</p>
 *
 * <p>Este controlador es el unico sitio donde se cruzan personas y pedidos, y por
 * eso inyecta dos repositorios: la capa 2 no se conoce a si misma en circulo.</p>
 *
 * <p>Capa: Controlador (API REST).</p>
 */
@RestController
@RequestMapping("/api/clientes")
public class ClientesApiControlador {

    private static final List<String> ESTADOS = List.of("activo", "inactivo");

    private final PersonaRepositorio personas;
    private final PedidoRepositorio pedidos;

    public ClientesApiControlador(PersonaRepositorio personas, PedidoRepositorio pedidos) {
        this.personas = personas;
        this.pedidos = pedidos;
    }

    /** GET /api/clientes — listado con el total de pedidos de cada uno. */
    @GetMapping
    public RespuestaApi listar() {
        Map<Integer, Integer> pedidosPorCliente = pedidos.listarRecientes(0).stream()
                .collect(Collectors.groupingBy(
                        (Pedido pedido) -> pedido.id_cliente,
                        Collectors.summingInt(pedido -> 1)));

        List<Cliente> clientes = personas.listarClientes();
        clientes.forEach(cliente -> cliente.total_pedidos =
                pedidosPorCliente.getOrDefault(cliente.id_cliente, 0));

        return RespuestaApi.ok(clientes);
    }

    /** PUT /api/clientes — activar o desactivar una cuenta. */
    @PutMapping
    public ResponseEntity<RespuestaApi> cambiarEstado(@RequestBody Map<String, Object> cuerpo) {
        int idCliente = PeticionUtil.entero(cuerpo, "id_cliente");
        String estado = PeticionUtil.texto(cuerpo, "estado_cliente", PeticionUtil.texto(cuerpo, "estado"));

        if (estado == null || !ESTADOS.contains(estado)) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Estado no valido: usa activo o inactivo"));
        }
        if (!personas.cambiarEstadoCliente(idCliente, estado)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Cliente no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(
                personas.buscarCliente(idCliente).orElse(null), "Cliente actualizado"));
    }
}
