// checkout.js — Flujo de pago en varios pasos

var pasoActual = 2;

function irAPaso(paso) {
    document.querySelectorAll('.contenido-paso').forEach(function(el) { el.style.display = 'none'; });
    var target = document.getElementById('paso-' + paso);
    if (target) target.style.display = 'block';
    pasoActual = paso;

    [2, 3, 4].forEach(function(n) {
        var ind = document.getElementById('indicador-paso-' + n);
        if (!ind) return;
        ind.className = 'paso' + (n < paso ? ' completado' : n === paso ? ' activo' : '');
    });

    if (paso === 4) llenarResumenConfirmacion();
}

function llenarResumenConfirmacion() {
    var nombre    = document.getElementById('nombre')?.value    || '';
    var apellido  = document.getElementById('apellido')?.value  || '';
    var direccion = document.getElementById('direccion')?.value || '';
    var barrio    = document.getElementById('barrio')?.value    || '';
    var pagoEl    = document.querySelector('input[name="pago"]:checked');
    var pagoNombre = pagoEl
        ? pagoEl.closest('.metodo-pago')?.querySelector('.metodo-pago-nombre')?.textContent
        : '—';

    var elNombre = document.getElementById('confirm-nombre');
    var elDir    = document.getElementById('confirm-direccion');
    var elPago   = document.getElementById('confirm-pago');
    var subtotal = calcularSubtotal();
    var descuento = obtenerDescuento();
    var total    = subtotal + 7000 - descuento;

    if (elNombre) elNombre.textContent = nombre + ' ' + apellido;
    if (elDir)    elDir.textContent    = direccion + (barrio ? ', ' + barrio : '');
    if (elPago)   elPago.textContent   = pagoNombre || '—';

    var elSub  = document.getElementById('confirm-subtotal');
    var elDesc = document.getElementById('confirm-descuento');
    var elDom  = document.getElementById('confirm-domicilio');
    var elTot  = document.getElementById('confirm-total');
    if (elSub)  elSub.textContent  = formatearPrecio(subtotal);
    if (elDesc) elDesc.textContent = descuento > 0 ? '-' + formatearPrecio(descuento) : '—';
    if (elDom)  elDom.textContent  = formatearPrecio(7000);
    if (elTot)  elTot.textContent  = formatearPrecio(Math.max(total, 0));
}

function confirmarPedido() {
    var usuario = obtenerSesion();
    if (!usuario) { window.location.href = 'login.html'; return; }

    var carrito = obtenerCarrito();
    if (carrito.length === 0) { mostrarToast('El carrito está vacío', 'error'); return; }

    var direccion    = document.getElementById('direccion')?.value    || '';
    var barrio       = document.getElementById('barrio')?.value       || '';
    var indicaciones = document.getElementById('indicaciones')?.value || '';

    if (!direccion) { mostrarToast('Escribe la dirección de entrega', 'error'); irAPaso(2); return; }

    // Leer el método de pago seleccionado
    var pagoEl     = document.querySelector('input[name="pago"]:checked');
    var metodoPago = pagoEl ? pagoEl.value : 'contraentrega';

    var items = carrito.map(function(i) {
        return { id_producto: i.id_producto, cantidad: i.cantidad, precio_unitario: i.precio };
    });

    var btnConfirmar = document.querySelector('[onclick="confirmarPedido()"]');
    if (btnConfirmar) { btnConfirmar.disabled = true; btnConfirmar.textContent = 'Procesando...'; }

    fetch(API + '/pedidos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            id_cliente:        usuario.id_entidad,
            items:             items,
            direccion_entrega: direccion + (barrio ? ', ' + barrio : ''),
            observaciones:     indicaciones,
            metodo_pago:       metodoPago
        })
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        if (resp.exito) {
            vaciarCarrito();
            localStorage.removeItem('fb_cupon');
            localStorage.removeItem('fb_cupon_data');
            localStorage.setItem('fb_pedido_confirmado', resp.datos.numero_factura);
            window.location.href = 'cuenta.html';
        } else {
            mostrarToast(resp.mensaje || 'Error al procesar el pedido', 'error');
            if (btnConfirmar) { btnConfirmar.disabled = false; btnConfirmar.textContent = '✅ Confirmar pedido'; }
        }
    })
    .catch(function() {
        mostrarToast('No se pudo conectar con el servidor', 'error');
        if (btnConfirmar) { btnConfirmar.disabled = false; btnConfirmar.textContent = '✅ Confirmar pedido'; }
    });
}

function seleccionarPago(elemento) {
    document.querySelectorAll('.metodo-pago').forEach(function(m) { m.classList.remove('seleccionado'); });
    elemento.classList.add('seleccionado');
    // Marcar el radio interno como checked
    var radio = elemento.querySelector('input[name="pago"]');
    if (radio) radio.checked = true;
}
