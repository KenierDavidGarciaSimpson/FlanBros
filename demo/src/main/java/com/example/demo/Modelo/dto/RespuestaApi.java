package com.example.demo.Modelo.dto;

import java.util.List;

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
        respuesta.datos = List.of();
        return respuesta;
    }
}
