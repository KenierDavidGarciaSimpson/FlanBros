package com.example.demo.Persistencia;

import java.util.List;
import java.util.Optional;

import com.example.demo.Modelo.Cupon;

public interface CuponRepositorio {

    List<Cupon> listar();

    Optional<Cupon> buscarPorCodigo(String codigo);
}
