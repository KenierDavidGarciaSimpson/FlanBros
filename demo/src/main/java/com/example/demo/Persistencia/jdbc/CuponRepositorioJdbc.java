package com.example.demo.Persistencia.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.Cupon;
import com.example.demo.Persistencia.CuponRepositorio;

@Repository
@Profile("!memoria")
public class CuponRepositorioJdbc implements CuponRepositorio {

    private static final String SELECT_CUPON =
            "SELECT codigo, descuento_pct, descripcion, activo FROM cupon";

    private final JdbcClient jdbc;

    public CuponRepositorioJdbc(JdbcTemplate plantilla) {
        this.jdbc = JdbcClient.create(plantilla);
    }

    @Override
    public List<Cupon> listar() {
        return jdbc.sql(SELECT_CUPON + " ORDER BY codigo")
                .query(CuponRepositorioJdbc::cupon).list();
    }

    @Override
    public Optional<Cupon> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return jdbc.sql(SELECT_CUPON + " WHERE activo = TRUE AND codigo = :codigo")
                .param("codigo", codigo.trim().toUpperCase(Locale.ROOT))
                .query(CuponRepositorioJdbc::cupon).optional();
    }

    private static Cupon cupon(ResultSet rs, int fila) throws SQLException {
        Cupon c = new Cupon();
        c.codigo = rs.getString("codigo");
        c.descuento_pct = rs.getInt("descuento_pct");
        c.descripcion = rs.getString("descripcion");
        c.activo = rs.getBoolean("activo");
        return c;
    }
}
