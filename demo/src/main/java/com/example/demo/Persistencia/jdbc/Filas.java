package com.example.demo.Persistencia.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Filas — utilidades para leer columnas con valores nulos desde un {@link ResultSet}.
 *
 * <p>Los modelos usan campos publicos (sin setters), por eso los repositorios JDBC
 * mapean cada fila a mano en lugar de usar un {@code BeanPropertyRowMapper}.</p>
 *
 * <p>Capa: Persistencia (JDBC).</p>
 */
final class Filas {

    private Filas() {
    }

    /** Entero que puede ser NULL en la base (por ejemplo, un domiciliario sin asignar). */
    static Integer enteroONulo(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    static LocalDateTime fechaHora(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDateTime.class);
    }

    static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDate.class);
    }
}
