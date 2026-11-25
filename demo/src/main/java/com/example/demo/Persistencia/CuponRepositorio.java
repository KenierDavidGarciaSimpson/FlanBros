package com.example.demo.Persistencia;

import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.Cupon;

/**
 * CuponRepositorio — capa 2 (Persistencia), INTERFAZ.
 *
 * <p>Los cupones estaban incrustados en {@code cupones.php} (un arreglo de tres
 * posiciones). Pasan a la capa de datos para que el controlador no tenga ni un
 * codigo escrito a mano.</p>
 *
 * <p>Hoy la implementacion lee {@code resources/datos/cupones.json}; el dia que
 * exista la tabla {@code Cupon} solo cambia esa clase.</p>
 */
public interface CuponRepositorio {

    List<Cupon> listar();

    /** Busca el codigo sin importar mayusculas ni espacios sobrantes. */
    Optional<Cupon> buscarPorCodigo(String codigo);
}
