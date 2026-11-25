// cupones.js — Cupones de descuento.
// El navegador solo muestra el descuento: el servidor vuelve a validar el código
// al crear el pedido (codigo_cupon) y es él quien lo aplica a la factura.

var _cuponAplicado = null;

function claveCupon() {
    var u = obtenerSesion();
    return u ? 'fb_cupon_' + (u.id_usuario || u.id_entidad) : null;
}

function cuponActual() {
    if (_cuponAplicado) return _cuponAplicado;
    var clave = claveCupon();
    if (!clave) return null;
    try { _cuponAplicado = JSON.parse(localStorage.getItem(clave)) || null; }
    catch (e) { _cuponAplicado = null; }
    return _cuponAplicado;
}

function obtenerDescuento() {
    var c = cuponActual();
    if (!c) return 0;
    return Math.round(calcularSubtotal() * (c.pct / 100));
}

function quitarCupon(silencioso) {
    _cuponAplicado = null;
    var clave = claveCupon();
    if (clave) localStorage.removeItem(clave);
    var input = document.getElementById('input-cupon');
    if (input) input.value = '';
    pintarEstadoCupon();
    if (typeof actualizarResumenCarrito === 'function') actualizarResumenCarrito();
    if (!silencioso) mostrarToast('Cupón quitado');
}

function pintarEstadoCupon() {
    var el = document.getElementById('estado-cupon');
    if (!el) return;
    var c = cuponActual();
    el.innerHTML = c
        ? '✓ Cupón <b>' + escaparHTML(c.codigo) + '</b> aplicado (' + c.pct + '%) · <a href="#" onclick="quitarCupon();return false" style="color:var(--caramelo)">quitar</a>'
        : '';
}

function aplicarCupon() {
    if (!exigirSesionParaComprar()) return;
    var input = document.getElementById('input-cupon');
    if (!input) return;
    var codigo = input.value.trim().toUpperCase();
    if (!codigo) { mostrarToast('Escribe un código de cupón', 'error'); return; }
    if (!/^[A-Z0-9]{3,30}$/.test(codigo)) { mostrarToast('El código solo lleva letras y números', 'error'); return; }
    if (obtenerCarrito().length === 0) { mostrarToast('Agrega productos antes de aplicar un cupón', 'error'); return; }

    fetch(API + '/cupones', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ codigo: codigo, subtotal: calcularSubtotal() })
    })
    .then(function (r) { return r.json(); })
    .then(function (resp) {
        if (resp.exito) {
            _cuponAplicado = { codigo: resp.datos.codigo, pct: resp.datos.descuento_pct };
            var clave = claveCupon();
            if (clave) localStorage.setItem(clave, JSON.stringify(_cuponAplicado));
            mostrarToast('✓ Cupón aplicado: ' + resp.datos.descuento_pct + '% de descuento');
            pintarEstadoCupon();
            if (typeof actualizarResumenCarrito === 'function') actualizarResumenCarrito();
        } else {
            mostrarToast(resp.mensaje || 'Cupón no válido', 'error');
        }
    })
    .catch(function () { mostrarToast('No se pudo validar el cupón. Revisa tu conexión.', 'error'); });
}
