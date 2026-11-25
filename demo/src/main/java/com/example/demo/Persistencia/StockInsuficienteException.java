package com.example.demo.Persistencia;

/**
 * StockInsuficienteException — capa 2 (Persistencia).
 *
 * <p>Se lanza cuando un pedido pide mas unidades de las que hay. Existe para que el
 * controlador no tenga que adivinar si un pedido se creo o no: si no se lanza, el
 * pedido existe completo (cabecera, lineas, factura, pago y domicilio).</p>
 *
 * <p>El mensaje es el mismo texto que devolvia {@code pedidos.php}
 * ("Stock insuficiente para el producto ID 3. Disponible: 2, solicitado: 5") y se
 * muestra tal cual al cliente en el aviso flotante.</p>
 */
public class StockInsuficienteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }

    /** Arma el mensaje con el formato del backend original. */
    public static StockInsuficienteException para(int idProducto, int disponible, int solicitado) {
        return new StockInsuficienteException(
                "Stock insuficiente para el producto ID " + idProducto
                        + ". Disponible: " + disponible + ", solicitado: " + solicitado);
    }
}
