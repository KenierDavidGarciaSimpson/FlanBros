// checkout.js — Flujo de pago en varios pasos (2: datos, 3: pago, 4: confirmar)

var pasoActual = 2;
var METODOS_VALIDOS = ['contraentrega', 'tarjeta', 'transferencia', 'billetera_digital'];

// Revisa los datos de entrega. Devuelve el primer error o null.
function validarDatosEntrega() {
    var v = function (id) { return (document.getElementById(id) || {}).value || ''; };
    var direccion = v('direccion').trim();
    var barrio    = v('barrio').trim();
    return VALIDA.primero(
        VALIDA.nombre('nombre', v('nombre')),
        VALIDA.nombre('apellido', v('apellido')),
        VALIDA.largo('dirección', direccion, 5, 150),
        VALIDA.largo('barrio', barrio, 0, 45),
        VALIDA.telefono(v('telefono'), true),
        VALIDA.largo('indicaciones', v('indicaciones'), 0, 250)
    );
}

function metodoPagoElegido() {
    var pagoEl = document.querySelector('input[name="pago"]:checked');
    return pagoEl ? pagoEl.value : null;
}

function irAPaso(paso) {
    // No se avanza con datos incompletos.
    if (paso > pasoActual) {
        if (pasoActual <= 2 && paso >= 3) {
            var error = validarDatosEntrega();
            if (error) { mostrarToast(error, 'error'); return; }
        }
        if (pasoActual <= 3 && paso >= 4 && METODOS_VALIDOS.indexOf(metodoPagoElegido()) === -1) {
            mostrarToast('Elige un método de pago', 'error'); return;
        }
    }

    document.querySelectorAll('.contenido-paso').forEach(function (el) { el.style.display = 'none'; });
    var target = document.getElementById('paso-' + paso);
    if (target) target.style.display = 'block';
    pasoActual = paso;

    [2, 3, 4].forEach(function (n) {
        var ind = document.getElementById('indicador-paso-' + n);
        if (!ind) return;
        ind.className = 'paso' + (n < paso ? ' completado' : n === paso ? ' activo' : '');
    });

    if (paso === 4) llenarResumenConfirmacion();
}

function llenarResumenConfirmacion() {
    var v = function (id) { return ((document.getElementById(id) || {}).value || '').trim(); };
    var pagoEl = document.querySelector('input[name="pago"]:checked');
    var pagoNombre = pagoEl ? pagoEl.closest('.metodo-pago')?.querySelector('.metodo-pago-nombre')?.textContent : '—';
    var t = calcularTotales();
    var poner = function (id, texto) { var el = document.getElementById(id); if (el) el.textContent = texto; };

    poner('confirm-nombre',    v('nombre') + ' ' + v('apellido'));
    poner('confirm-direccion', v('direccion') + (v('barrio') ? ', ' + v('barrio') : ''));
    poner('confirm-pago',      pagoNombre || '—');
    poner('confirm-subtotal',  formatearPrecio(t.subtotal));
    poner('confirm-descuento', t.descuento > 0 ? '-' + formatearPrecio(t.descuento) : '—');
    poner('confirm-iva',       formatearPrecio(t.iva));
    poner('confirm-domicilio', formatearPrecio(t.domicilio) + ' (al recibir)');
    poner('confirm-total',     formatearPrecio(t.total));
}

function confirmarPedido() {
    var usuario = obtenerSesion();
    if (!usuario) { window.location.href = 'login.html'; return; }
    if (usuario.nombre_rol !== 'cliente') { mostrarToast('Solo las cuentas de cliente pueden hacer pedidos', 'error'); return; }
    if (obtenerCarrito().length === 0) { mostrarToast('El carrito está vacío', 'error'); return; }

    var error = validarDatosEntrega();
    if (error) { mostrarToast(error, 'error'); irAPaso(2); return; }
    var metodoPago = metodoPagoElegido();
    if (METODOS_VALIDOS.indexOf(metodoPago) === -1) { mostrarToast('Elige un método de pago', 'error'); irAPaso(3); return; }

    var btn = document.getElementById('btn-confirmar-pedido') || document.querySelector('[onclick="confirmarPedido()"]');
    var textoBtn = btn ? btn.textContent : '';
    var restaurar = function () { if (btn) { btn.disabled = false; btn.textContent = textoBtn; } };
    if (btn) { btn.disabled = true; btn.textContent = 'Procesando...'; }

    // Antes de enviar, confirmar precios y stock con el servidor.
    sincronizarCarrito().then(function (cambio) {
        if (cambio) {
            mostrarResumenCheckout();
            llenarResumenConfirmacion();
            restaurar();
            if (obtenerCarrito().length === 0) { window.location.href = 'carrito.html'; }
            return;
        }

        var v = function (id) { return ((document.getElementById(id) || {}).value || '').trim(); };
        var barrio = v('barrio');
        var telefono = v('telefono');
        var indicaciones = v('indicaciones');
        var observaciones = 'Tel: ' + telefono + (indicaciones ? ' | ' + indicaciones : '');
        var cupon = (typeof cuponActual === 'function') ? cuponActual() : null;

        var cuerpo = {
            items: obtenerCarrito().map(function (i) { return { id_producto: i.id_producto, cantidad: i.cantidad }; }),
            direccion_entrega: v('direccion') + (barrio ? ', ' + barrio : ''),
            observaciones: observaciones,
            metodo_pago: metodoPago,
            codigo_cupon: cupon ? cupon.codigo : null
        };

        return fetch(API + '/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(cuerpo)
        })
        .then(function (r) { return r.json(); })
        .then(function (resp) {
            if (resp.exito) {
                vaciarCarrito();
                localStorage.setItem('fb_pedido_confirmado', resp.datos.numero_factura);
                window.location.href = 'cuenta.html';
            } else {
                mostrarToast(resp.mensaje || 'Error al procesar el pedido', 'error');
                restaurar();
            }
        });
    }).catch(function () {
        mostrarToast('No se pudo conectar con el servidor', 'error');
        restaurar();
    });
}

function seleccionarPago(elemento) {
    document.querySelectorAll('.metodo-pago').forEach(function (m) { m.classList.remove('seleccionado'); });
    elemento.classList.add('seleccionado');
    var radio = elemento.querySelector('input[name="pago"]');
    if (radio) radio.checked = true;
}

// Resumen lateral del checkout.
function mostrarResumenCheckout() {
    var carrito = obtenerCarrito();
    var contenedor = document.getElementById('items-checkout');
    if (!contenedor) return;

    if (carrito.length === 0) {
        contenedor.innerHTML = '<p style="color:var(--texto-claro);font-size:13px">Carrito vacío. <a href="catalogo.html" style="color:var(--caramelo)">Ver catálogo</a></p>';
    } else {
        contenedor.innerHTML = carrito.map(function (item) {
            return '<div style="display:flex;align-items:center;gap:10px;margin-bottom:10px">'
                + '<div style="width:40px;height:40px;background:var(--crema-2);border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:22px">' + escaparHTML(item.emoji || '🍰') + '</div>'
                + '<div style="flex:1"><div style="font-size:13px;font-weight:600;color:var(--chocolate)">' + escaparHTML(item.nombre) + '</div>'
                + '<div style="font-size:12px;color:var(--texto-claro)">x' + item.cantidad + '</div></div>'
                + '<span style="font-family:var(--fuente-titulo);font-size:15px;color:var(--caramelo)">' + formatearPrecio(item.precio * item.cantidad) + '</span>'
                + '</div>';
        }).join('');
    }

    var t = calcularTotales();
    var poner = function (id, texto) { var el = document.getElementById(id); if (el) el.textContent = texto; };
    poner('checkout-subtotal',  formatearPrecio(t.subtotal));
    poner('checkout-descuento', t.descuento > 0 ? '-' + formatearPrecio(t.descuento) : '—');
    poner('checkout-iva',       formatearPrecio(t.iva));
    poner('checkout-total',     formatearPrecio(t.total));
}

// Arranque de checkout.html
function iniciarCheckout() {
    var u = obtenerSesion();
    if (!u) { requiereSesion(); return; }
    if (obtenerCarrito().length === 0) { window.location.href = 'carrito.html'; return; }
    var poner = function (id, valor) { var el = document.getElementById(id); if (el && !el.value) el.value = valor || ''; };
    poner('nombre', u.nombre);
    poner('apellido', u.apellido);
    sincronizarCarrito().then(function () {
        if (obtenerCarrito().length === 0) { window.location.href = 'carrito.html'; return; }
        mostrarResumenCheckout();
    });
    mostrarResumenCheckout();
    pasoActual = 2;
    irAPaso(2);
}
