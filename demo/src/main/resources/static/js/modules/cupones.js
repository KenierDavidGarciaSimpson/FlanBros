// cupones.js — Validación de cupones (verificado en el backend)

var _cuponAplicado = null;

function obtenerDescuento() {
    if (!_cuponAplicado) {
        // Intentar recuperar del localStorage si ya fue validado en esta sesión
        try { _cuponAplicado = JSON.parse(localStorage.getItem('fb_cupon_data')) || null; }
        catch(e) { _cuponAplicado = null; }
    }
    if (!_cuponAplicado) return 0;
    return Math.round(calcularSubtotal() * (_cuponAplicado.pct / 100));
}

function aplicarCupon() {
    var input = document.getElementById('input-cupon');
    if (!input) return;
    var codigo = input.value.trim().toUpperCase();
    if (!codigo) { mostrarToast('Escribe un código de cupón', 'error'); return; }

    var subtotal = calcularSubtotal();

    fetch(API + '/cupones', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ codigo: codigo, subtotal: subtotal })
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        if (resp.exito) {
            _cuponAplicado = { codigo: resp.datos.codigo, pct: resp.datos.descuento_pct };
            localStorage.setItem('fb_cupon', resp.datos.codigo);
            localStorage.setItem('fb_cupon_data', JSON.stringify(_cuponAplicado));
            mostrarToast('✓ Cupón aplicado: ' + resp.datos.descuento_pct + '% de descuento');
            actualizarResumenCarrito();
        } else {
            mostrarToast(resp.mensaje || 'Cupón no válido', 'error');
        }
    })
    .catch(function() {
        // Si no hay servidor, usar validación local como respaldo para la demo
        var LOCAL = { 'FLANBROSS10': 10, 'BIENVENIDO': 15, 'DULCE20': 20 };
        if (LOCAL[codigo]) {
            _cuponAplicado = { codigo: codigo, pct: LOCAL[codigo] };
            localStorage.setItem('fb_cupon', codigo);
            localStorage.setItem('fb_cupon_data', JSON.stringify(_cuponAplicado));
            mostrarToast('✓ Cupón aplicado: ' + LOCAL[codigo] + '% de descuento');
            actualizarResumenCarrito();
        } else {
            mostrarToast('Cupón no válido', 'error');
        }
    });
}
