function cargarDashboard() {
    fetch('/api/dashboard')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            if (!resp.exito) return;
            var d = resp.datos;

            setText('kpi-ventas',     formatearPrecio(d.ventas_mes));
            setText('kpi-pedidos',    d.pedidos_mes);
            setText('kpi-clientes',   d.clientes_activos);
            setText('kpi-domicilios', d.domicilios_activos);

            var elPed = document.getElementById('tabla-ultimos-pedidos');
            if (elPed && d.ultimos_pedidos) {
                elPed.innerHTML = d.ultimos_pedidos.map(function(p) {
                    return '<tr>'
                        + '<td>' + (p.numero_factura || '#' + p.id_pedido) + '</td>'
                        + '<td>' + p.nombre + ' ' + p.apellido + '</td>'
                        + '<td><span class="estado-badge">' + etiquetaEstado(p.estado_pedido) + '</span></td>'
                        + '<td>' + formatearPrecio(p.total_pedido) + '</td>'
                        + '</tr>';
                }).join('');
            }

            var elStock = document.getElementById('lista-stock-critico');
            if (elStock) {
                elStock.innerHTML = d.stock_critico.length === 0
                    ? '<p style="color:var(--texto-claro);font-size:13px">Stock en niveles normales.</p>'
                    : d.stock_critico.map(function(s) {
                        return '<div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--borde);font-size:13px">'
                            + '<span>' + s.nombre_producto + '</span>'
                            + '<span class="estado-badge">' + s.cantidad_disponible + ' uds</span>'
                            + '</div>';
                    }).join('');
            }
        })
        .catch(function() { mostrarToast('Error cargando dashboard', 'error'); });
}
