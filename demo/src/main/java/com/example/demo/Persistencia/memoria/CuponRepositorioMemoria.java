package com.example.demo.Persistencia.memoria;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.demo.Modelo.Cupon;
import com.example.demo.Persistencia.CuponRepositorio;

@Profile("memoria")
@Repository
public class CuponRepositorioMemoria implements CuponRepositorio {

    private final List<Cupon> cupones;

    public CuponRepositorioMemoria(LectorJson lector) {
        this.cupones = new CopyOnWriteArrayList<>(lector.leerLista("datos/cupones.json", Cupon.class));
    }

    @Override
    public List<Cupon> listar() {
        return List.copyOf(cupones);
    }

    @Override
    public Optional<Cupon> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String buscado = codigo.trim().toUpperCase(Locale.ROOT);
        return cupones.stream()
                .filter(cupon -> cupon.activo)
                .filter(cupon -> cupon.codigoNormalizado().equals(buscado))
                .findFirst();
    }
}
