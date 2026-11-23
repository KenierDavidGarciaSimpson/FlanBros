package com.example.demo.Controlador.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
