package com.example.demo.Persistencia.memoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.MensajeContacto;
import com.example.demo.Persistencia.ContactoRepositorio;

@Profile("memoria")
@Repository
public class ContactoRepositorioMemoria implements ContactoRepositorio {

    private final List<MensajeContacto> mensajes = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public synchronized MensajeContacto guardar(MensajeContacto mensaje) {
        mensaje.id_mensaje = siguienteId++;
        if (mensaje.fecha_envio == null) {
            mensaje.fecha_envio = LocalDateTime.now();
        }
        mensaje.estado = "sin_leer";
        mensajes.add(mensaje);
        return mensaje;
    }

    @Override
    public synchronized List<MensajeContacto> listar() {
        return List.copyOf(mensajes);
    }

    @Override
    public synchronized boolean marcarLeido(int idMensaje) {
        for (MensajeContacto m : mensajes) {
            if (m.id_mensaje == idMensaje) {
                m.estado = "leido";
                return true;
            }
        }
        return false;
    }
}
