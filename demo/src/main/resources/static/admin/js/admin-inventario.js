// admin-inventario.js — Stock disponible por producto

function cargarInventarioAdmin() {
    fetch('/api/inventario')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-inventario');
            if (!el || !resp.exito) return;
            el.innerHTML = resp.datos.map(function(i) {
                return '<tr>'
                    + '<td>' + i.nombre_producto + '</td>'
                    + '<td>' + i.categoria + '</td>'
                    + '<td><input type="number" value="' + i.cantidad_disponible + '" min="0" style="width:70px;padding:4px 8px;border:1px solid var(--borde);border-radius:6px" id="inv-' + i.id_producto + '"></td>'
                    + '<td>' + i.cantidad_minima + '</td>'
                    + '<td><span class="estado-badge">' + i.estado_inventario + '</span></td>'
                    + '<td><button class="btn-sm btn-sm-borde" onclick="actualizarStock(' + i.id_producto + ')">Guardar</button></td>'
                    + '</tr>';
            }).join('');
        });
}

function actualizarStock(id) {
    var val = document.getElementById('inv-' + id)?.value;
    fetch('/api/inventario', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_producto: id, cantidad_disponible: parseInt(val) })
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        mostrarToast(r.exito ? 'Stock actualizado' : r.mensaje, r.exito ? 'ok' : 'error');
        if (r.exito) cargarInventarioAdmin();
    });
}
