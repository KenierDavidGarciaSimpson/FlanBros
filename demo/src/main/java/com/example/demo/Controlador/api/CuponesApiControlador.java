package com.example.demo.Controlador.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.Cupon;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.CuponRepositorio;

@RestController
@RequestMapping("/api/cupones")
public class CuponesApiControlador {

    private final CuponRepositorio cupones;

    public CuponesApiControlador(CuponRepositorio cupones) {
        this.cupones = cupones;
    }

    @PostMapping
    public ResponseEntity<RespuestaApi> validar(@RequestBody Map<String, Object> cuerpo) {
        String codigo = PeticionUtil.texto(cuerpo, "codigo");
        Optional<Cupon> encontrado = cupones.buscarPorCodigo(codigo);
        if (encontrado.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Cupon invalido o vencido"));
        }

        Cupon cupon = encontrado.get();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", cupon.codigo);
        datos.put("descuento_pct", cupon.descuento_pct);
        datos.put("descripcion", cupon.descripcion);

        BigDecimal subtotal = PeticionUtil.decimal(cuerpo, "subtotal");
        if (subtotal != null) {
            datos.put("descuento", cupon.descuentoPara(subtotal));
        }

        return ResponseEntity.ok(RespuestaApi.ok(datos, "Cupon aplicado"));
    }
}
