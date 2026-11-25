// admin-clientes.js — Listado y activación/desactivación de clientes

function cargarClientesAdmin() {
    fetch('/api/clientes')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-clientes');
            if (!el || !resp.exito) return;
            el.innerHTML = resp.datos.map(function(c) {
                return '<tr>'
                    + '<td>' + c.nombre + ' ' + (c.apellido || '') + '</td>'
                    + '<td>' + c.correo + '</td>'
                    + '<td>' + (c.telefono || '—') + '</td>'
                    + '<td>' + (c.total_pedidos || 0) + '</td>'
                    + '<td><span class="estado-badge">' + c.estado_cliente + '</span></td>'
                    + '<td><button class="btn-sm btn-sm-borde" onclick="toggleCliente(' + c.id_cliente + ',\'' + c.estado_cliente + '\')">'
                    + (c.estado_cliente === 'activo' ? 'Desactivar' : 'Activar') + '</button></td>'
                    + '</tr>';
            }).join('');
        });
}

function toggleCliente(id, est) {
    var nuevo = est === 'activo' ? 'inactivo' : 'activo';
    fetch('/api/clientes', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_cliente: id, estado: nuevo })
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        mostrarToast(r.exito ? 'Cliente actualizado' : r.mensaje, r.exito ? 'ok' : 'error');
        if (r.exito) cargarClientesAdmin();
    });
}
