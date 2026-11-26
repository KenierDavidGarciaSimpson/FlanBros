function cargarDomiciliosAdmin() {
    fetch('/api/domicilios')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-domicilios');
            if (!el || !resp.exito) return;
            if (resp.datos.length === 0) {
                el.innerHTML = '<tr><td colspan="6" style="padding:16px;color:var(--texto-claro)">No hay domicilios registrados.</td></tr>';
                return;
            }
            el.innerHTML = resp.datos.map(function(d) {
                return '<tr>'
                    + '<td>' + (d.numero_factura || 'Pedido #' + d.id_pedido) + '</td>'
                    + '<td>' + d.nombre + ' ' + (d.apellido || '') + '<br><span style="font-size:11px;color:var(--texto-claro)">' + (d.telefono || '') + '</span></td>'
                    + '<td>' + d.direccion_entrega + '</td>'
                    + '<td>' + (d.nombre_domiciliario || '<em style="color:var(--texto-claro)">Sin asignar</em>') + '</td>'
                    + '<td><span class="estado-badge">' + d.estado_entrega + '</span></td>'
                    + '<td>' + formatearPrecio(d.costo_domicilio) + '</td>'
                    + '</tr>';
            }).join('');
        })
        .catch(function() { mostrarToast('Error cargando domicilios', 'error'); });
}
