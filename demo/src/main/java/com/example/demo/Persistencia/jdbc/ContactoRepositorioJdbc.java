package com.example.demo.Persistencia.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.MensajeContacto;
import com.example.demo.Persistencia.ContactoRepositorio;

/**
 * ContactoRepositorioJdbc — capa 2 (Persistencia), buzon de contacto en PostgreSQL.
 *
 * <p>Capa: Persistencia (JDBC). Activo en cualquier perfil distinto de {@code memoria}.</p>
 */
@Repository
@Profile("!memoria")
public class ContactoRepositorioJdbc implements ContactoRepositorio {

    private final JdbcClient jdbc;

    public ContactoRepositorioJdbc(JdbcTemplate plantilla) {
        this.jdbc = JdbcClient.create(plantilla);
    }

    @Override
    public MensajeContacto guardar(MensajeContacto mensaje) {
        if (mensaje.fecha_envio == null) {
            mensaje.fecha_envio = LocalDateTime.now();
        }
        mensaje.estado = "sin_leer";
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO mensaje_contacto (nombre, correo, telefono, asunto, mensaje,
                                              fecha_envio, estado)
                VALUES (:nombre, :correo, :telefono, :asunto, :mensaje, :fecha, 'sin_leer')
                """)
                .param("nombre", mensaje.nombre)
                .param("correo", mensaje.correo)
                .param("telefono", mensaje.telefono)
                .param("asunto", mensaje.asunto)
                .param("mensaje", mensaje.mensaje)
                .param("fecha", mensaje.fecha_envio)
                .update(llave, "id_mensaje");
        mensaje.id_mensaje = llave.getKey().intValue();
        return mensaje;
    }

    @Override
    public List<MensajeContacto> listar() {
        return jdbc.sql("""
                SELECT id_mensaje, nombre, correo, telefono, asunto, mensaje, fecha_envio, estado
                  FROM mensaje_contacto
                 ORDER BY id_mensaje
                """)
                .query(ContactoRepositorioJdbc::mensaje).list();
    }

    private static MensajeContacto mensaje(ResultSet rs, int fila) throws SQLException {
        MensajeContacto m = new MensajeContacto();
        m.id_mensaje = rs.getInt("id_mensaje");
        m.nombre = rs.getString("nombre");
        m.correo = rs.getString("correo");
        m.telefono = rs.getString("telefono");
        m.asunto = rs.getString("asunto");
        m.mensaje = rs.getString("mensaje");
        m.fecha_envio = Filas.fechaHora(rs, "fecha_envio");
        m.estado = rs.getString("estado");
        return m;
    }
}
