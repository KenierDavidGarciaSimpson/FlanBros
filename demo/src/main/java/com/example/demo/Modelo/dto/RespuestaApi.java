package com.example.demo.Modelo.dto;

import java.util.List;

/**
 * RespuestaApi — capa 1 (Modelo), objeto de transferencia.
 *
 * <p>Es la MISMA forma de respuesta que devolvian {@code responderOK()} y
 * {@code responderError()} de {@code backend/config/helpers.php}:</p>
 *
 * <pre>{ "exito": true, "mensaje": "Cupón aplicado", "datos": { ... } }</pre>
 *
 * <p>Todo el JavaScript del proyecto hace {@code resp.exito} y {@code resp.datos},
 * asi que esta clase es el contrato entre la capa Controlador y las Vistas. Si
 * cambia aqui, cambia en 20 archivos: no se toca a la ligera.</p>
 *
 * <p>Capa: Modelo (DTO). No importa nada de Spring.</p>
 */
public class RespuestaApi {

    public boolean exito;
    public String mensaje;
    public Object datos;

    public static RespuestaApi ok(Object datos) {
        return ok(datos, "OK");
    }

    public static RespuestaApi ok(Object datos, String mensaje) {
        RespuestaApi respuesta = new RespuestaApi();
        respuesta.exito = true;
        respuesta.mensaje = mensaje;
        respuesta.datos = datos;
        return respuesta;
    }

    public static RespuestaApi error(String mensaje) {
        RespuestaApi respuesta = new RespuestaApi();
        respuesta.exito = false;
        respuesta.mensaje = mensaje;
        respuesta.datos = List.of(); // el PHP respondia "datos": []
        return respuesta;
    }
}
