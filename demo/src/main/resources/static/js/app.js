// app.js — Punto de entrada principal de FlanBross

// --- Módulos ---
// modules/utils.js     → formatearPrecio, formatearFecha, slugificar, mostrarToast, etiquetaEstado
// modules/sesion.js    → obtenerSesion, guardarSesion, cerrarSesion, requiereSesion, requiereAdmin
// modules/nav.js       → actualizarNav
// modules/carrito.js   → agregarAlCarrito, renderizarCarrito, vaciarCarrito, calcularSubtotal, actualizarBadgeCarrito
// modules/cupones.js   → aplicarCupon, obtenerDescuento
// modules/catalogo.js  → cargarCatalogo, filtrarProductos, seleccionarCategoria
// modules/checkout.js  → irAPaso, confirmarPedido, seleccionarPago
// modules/cuenta.js    → cargarMiCuenta

document.addEventListener('DOMContentLoaded', function() {
    actualizarBadgeCarrito();
    actualizarNav();
});
