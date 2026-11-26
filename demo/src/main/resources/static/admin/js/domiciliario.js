var _u = obtenerSesion();
if (!_u || _u.nombre_rol !== 'domiciliario') {
    window.location.href = '../login.html';
}

var _elNom = document.getElementById('nombre-domiciliario');
var _elAva = document.getElementById('avatar-domiciliario');
if (_elNom && _u) _elNom.textContent = _u.nombre + ' ' + (_u.apellido || '');
if (_elAva && _u) _elAva.textContent = ((_u.nombre || '')[0] + (_u.apellido || '')[0]).toUpperCase();

cargarDomicilios();

function cargarDomicilios() {
    fetch('/api/pedidos')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            if (!resp.exito) return;
            var pedidos = resp.datos.filter(function(p) {
                return p.estado_pedido === 'listo' || p.estado_pedido === 'en_preparacion';
            });

            fetch('/api/domicilios?id_domiciliario=' + _u.id_entidad)
                .then(function(r2) { return r2.json(); })
                .then(function(resp2) {
                    var domEnCamino = resp2.exito ? resp2.datos : [];
                    renderizarDomicilios(pedidos, domEnCamino);
                    actualizarStatsDOM(pedidos, domEnCamino);
                })
                .catch(function() {
                    renderizarDomicilios(pedidos, []);
                    actualizarStatsDOM(pedidos, []);
                });
        })
        .catch(function() { mostrarToast('No se pudo cargar los pedidos', 'error'); });
}

function renderizarDomicilios(pedidosListos, domEnCamino) {
    var lista = document.getElementById('lista-domicilios');
    if (!lista) return;

    var total = pedidosListos.length + domEnCamino.length;
    if (total === 0) {
        lista.innerHTML = '<div style="padding:24px;text-align:center;color:rgba(255,255,255,0.4);font-size:14px">No hay domicilios pendientes por ahora.</div>';
        return;
    }

    var html = '';

    domEnCamino.forEach(function(d) {
        html += '<div class="domicilio-card" id="dom-card-d' + d.id_domicilio + '">'
            + '<div class="domicilio-card-header">'
            + '<span style="font-weight:700;color:white;font-size:14px">' + (d.numero_factura || 'Pedido #' + d.id_pedido) + '</span>'
            + '<span class="badge-estado-dom en-camino">En camino</span>'
            + '</div>'
            + '<div style="font-size:13px;color:rgba(255,255,255,0.6);margin:6px 0">' + d.nombre + ' ' + (d.apellido || '') + '</div>'
            + '<div style="font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:6px">' + d.direccion_entrega + '</div>'
            + '<div style="font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:12px">' + formatearPrecio(d.total_pedido) + '</div>'
            + '<div style="display:flex;gap:8px">'
            + '<button class="btn-dom btn-dom-exito" onclick="marcarEntregado(' + d.id_pedido + ',' + d.id_domicilio + ')">✓ Marcar entregado</button>'
            + '</div>'
            + '</div>';
    });

    pedidosListos.forEach(function(p) {
        html += '<div class="domicilio-card" id="dom-card-' + p.id_pedido + '">'
            + '<div class="domicilio-card-header">'
            + '<span style="font-weight:700;color:white;font-size:14px">' + (p.numero_factura || 'Pedido #' + p.id_pedido) + '</span>'
            + '<span class="badge-estado-dom listo">Listo</span>'
            + '</div>'
            + '<div style="font-size:13px;color:rgba(255,255,255,0.6);margin:6px 0">' + p.nombre + ' ' + (p.apellido || '') + '</div>'
            + '<div style="font-size:12px;color:rgba(255,255,255,0.4);margin-bottom:12px">' + formatearFecha(p.fecha_pedido) + ' · ' + formatearPrecio(p.total_pedido) + '</div>'
            + '<div style="display:flex;gap:8px">'
            + '<button class="btn-dom btn-dom-primario" onclick="tomarDomicilio(' + p.id_pedido + ')">Salir a entregar</button>'
            + '</div>'
            + '</div>';
    });

    lista.innerHTML = html;
}

function actualizarStatsDOM(pedidosListos, domEnCamino) {
    var elPend  = document.getElementById('stat-dom-pendientes');
    var elCamin = document.getElementById('stat-dom-encamino');
    if (elPend)  elPend.textContent  = pedidosListos.length;
    if (elCamin) elCamin.textContent = domEnCamino.length;
}

function tomarDomicilio(idPedido) {
    fetch('/api/domicilios', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            id_pedido: idPedido,
            estado_entrega: 'en_camino',
            id_domiciliario: _u.id_entidad
        })
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        return fetch('/api/pedidos', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id_pedido: idPedido, estado: 'listo' })
        });
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        mostrarToast('Pedido en camino');
        cargarDomicilios();
    })
    .catch(function() { mostrarToast('Error al actualizar', 'error'); });
}

function marcarEntregado(idPedido, idDomicilio) {
    fetch('/api/pedidos', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_pedido: idPedido, estado: 'entregado' })
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        if (resp.exito) {
            mostrarToast('¡Pedido entregado!');
            var card = document.getElementById('dom-card-d' + idDomicilio) || document.getElementById('dom-card-' + idPedido);
            if (card) {
                card.style.opacity = '0';
                card.style.transition = 'opacity 0.4s';
                setTimeout(function() { cargarDomicilios(); }, 450);
            }
        } else {
            mostrarToast(resp.mensaje || 'Error', 'error');
        }
    })
    .catch(function() { mostrarToast('No se pudo conectar', 'error'); });
}
