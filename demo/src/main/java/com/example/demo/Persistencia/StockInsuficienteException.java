package com.example.demo.Persistencia;

public class StockInsuficienteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }

    public static StockInsuficienteException para(int idProducto, int disponible, int solicitado) {
        return new StockInsuficienteException(
                "Stock insuficiente para el producto ID " + idProducto
                        + ". Disponible: " + disponible + ", solicitado: " + solicitado);
    }
}
