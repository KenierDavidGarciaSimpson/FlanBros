package com.example.demo.Persistencia;

import java.util.List;

import com.example.demo.Modelo.MensajeContacto;

/**
 * ContactoRepositorio — capa 2 (Persistencia), INTERFAZ.
 *
 * <p>Guarda los mensajes del formulario de contacto (tabla
 * {@code Mensaje_Contacto}). Antes el formulario de contacto funcionaba solo con
 * PHP; sin el, este contrato permite que el mensaje se conserve mientras el
 * servidor siga arriba.</p>
 */
public interface ContactoRepositorio {

    MensajeContacto guardar(MensajeContacto mensaje);

    List<MensajeContacto> listar();

    boolean marcarLeido(int idMensaje);
}
