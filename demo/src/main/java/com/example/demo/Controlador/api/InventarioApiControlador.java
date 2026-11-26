package com.example.demo.Controlador.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.Inventario;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.ProductoRepositorio;

@RestController
@RequestMapping("/api/inventario")
public class InventarioApiControlador {

    private final ProductoRepositorio productos;

    public InventarioApiControlador(ProductoRepositorio productos) {
        this.productos = productos;
    }

    @GetMapping
    public RespuestaApi listar() {
        return RespuestaApi.ok(productos.listarInventario());
    }

    @PutMapping
    public ResponseEntity<RespuestaApi> actualizar(@RequestBody Map<String, Object> cuerpo) {
        int idProducto = PeticionUtil.entero(cuerpo, "id_producto");
        if (!cuerpo.containsKey("cantidad_disponible")) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Falta la cantidad disponible"));
        }
        int cantidad = Math.max(PeticionUtil.entero(cuerpo, "cantidad_disponible"), 0);

        Optional<Inventario> fila = productos.buscarInventario(idProducto);
        if (fila.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Ese producto no tiene fila de inventario"));
        }

        String estado = productos.actualizarInventario(idProducto, cantidad);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id_producto", idProducto);
        respuesta.put("cantidad_disponible", cantidad);
        respuesta.put("estado_inventario", estado);
        respuesta.put("mensaje", "Inventario actualizado");

        return ResponseEntity.ok(RespuestaApi.ok(respuesta, "Inventario actualizado correctamente"));
    }
}
