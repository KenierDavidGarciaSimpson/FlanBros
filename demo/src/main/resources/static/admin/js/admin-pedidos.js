function cargarPedidosAdmin() {
    fetch('/api/pedidos')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-pedidos');
            if (!el || !resp.exito) return;
            el.innerHTML = resp.datos.map(function(p) {
                var opts = ['pendiente','en_preparacion','listo','entregado','cancelado'].map(function(e) {
                    return '<option value="' + e + '"' + (e === p.estado_pedido ? ' selected' : '') + '>' + etiquetaEstado(e) + '</option>';
                }).join('');
                return '<tr>'
                    + '<td>' + (p.numero_factura || '#' + p.id_pedido) + '</td>'
                    + '<td>' + p.nombre + ' ' + p.apellido + '</td>'
                    + '<td>' + formatearFecha(p.fecha_pedido) + '</td>'
                    + '<td>' + formatearPrecio(p.total_pedido) + '</td>'
                    + '<td><select onchange="cambiarEstadoPedido(' + p.id_pedido + ',this.value)" style="font-size:12px;padding:4px 8px;border-radius:6px;border:1px solid var(--borde)">' + opts + '</select></td>'
                    + '</tr>';
            }).join('');
        });
}

function cambiarEstadoPedido(id, estado) {
    fetch('/api/pedidos', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_pedido: id, estado: estado })
    })
    .then(function(r) { return r.json(); })
    .then(function(r) { mostrarToast(r.exito ? 'Estado actualizado' : r.mensaje, r.exito ? 'ok' : 'error'); });
}
