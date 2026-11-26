// admin-facturas.js — Tabla de facturas emitidas

function cargarFacturasAdmin() {
    fetch('/api/facturas')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-facturas');
            if (!el || !resp.exito) return;
            el.innerHTML = resp.datos.map(function(f) {
                return '<tr>'
                    + '<td>' + f.numero_factura + '</td>'
                    + '<td>' + f.nombre + ' ' + f.apellido + '</td>'
                    + '<td>' + formatearFecha(f.fecha_factura) + '</td>'
                    + '<td>' + formatearPrecio(f.subtotal) + '</td>'
                    + '<td>' + formatearPrecio(f.impuestos) + '</td>'
                    + '<td>' + formatearPrecio(f.total_factura) + '</td>'
                    + '<td><span class="estado-badge">' + f.estado_factura + '</span></td>'
                    + '<td><button class="btn-sm btn-sm-principal" onclick="verFactura(' + f.id_pedido + ')">Ver factura</button></td>'
                    + '</tr>';
            }).join('');
        });
}
