package com.example.demo.Persistencia;

import java.util.List;

import com.example.demo.Modelo.MensajeContacto;

public interface ContactoRepositorio {

    MensajeContacto guardar(MensajeContacto mensaje);

    List<MensajeContacto> listar();

    boolean marcarLeido(int idMensaje);
}
