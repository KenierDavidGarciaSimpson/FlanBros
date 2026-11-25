// admin-pagos.js — Tabla de pagos registrados

function cargarPagosAdmin() {
    fetch('/api/pagos')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-pagos');
            if (!el || !resp.exito) return;
            if (resp.datos.length === 0) {
                el.innerHTML = '<tr><td colspan="7" style="padding:16px;color:var(--texto-claro)">No hay pagos registrados aún.</td></tr>';
                return;
            }
            el.innerHTML = resp.datos.map(function(p) {
                var metodosLabel = {
                    'tarjeta': '💳 Tarjeta',
                    'billetera_digital': '📱 Billetera',
                    'transferencia': '🏦 Transferencia',
                    'efectivo': '💵 Efectivo',
                    'contraentrega': '🚴 Contraentrega'
                };
                return '<tr>'
                    + '<td>' + p.numero_factura + '</td>'
                    + '<td>' + p.nombre + ' ' + (p.apellido || '') + '</td>'
                    + '<td>' + formatearFecha(p.fecha_pago) + '</td>'
                    + '<td>' + (metodosLabel[p.metodo_pago] || p.metodo_pago) + '</td>'
                    + '<td>' + formatearPrecio(p.monto_pago) + '</td>'
                    + '<td>' + (p.referencia_pago || '—') + '</td>'
                    + '<td><span class="estado-badge estado-' + p.estado_pago + '">' + p.estado_pago + '</span></td>'
                    + '</tr>';
            }).join('');
        })
        .catch(function() { mostrarToast('Error cargando pagos', 'error'); });
}
