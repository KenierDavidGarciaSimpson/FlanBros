// factura-detalle.js — muestra la factura completa de un pedido en una ventana y permite imprimirla.
// La usan "Mi cuenta" (cliente) y el panel (administrador y contador). El servidor decide
// quién puede ver qué: el cliente solo recibe sus propias facturas.
(function () {
    var METODOS = {
        contraentrega: 'Contraentrega', tarjeta: 'Tarjeta',
        transferencia: 'Transferencia', billetera_digital: 'Billetera digital'
    };

    function esc(t) {
        return String(t == null ? '' : t)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function pesos(n) {
        var v = Math.round(parseFloat(n || 0));
        return '$' + v.toLocaleString('es-CO');
    }

    function fecha(f) {
        if (!f) return '—';
        var d = new Date(f);
        return isNaN(d) ? String(f) : d.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
    }

    function htmlFactura(d) {
        var f = d.factura;
        var filas = (d.lineas || []).map(function (l) {
            return '<tr><td>' + esc(l.nombre_producto) + '</td>'
                + '<td style="text-align:center">' + l.cantidad + '</td>'
                + '<td style="text-align:right">' + pesos(l.precio_unitario) + '</td>'
                + '<td style="text-align:right">' + pesos(l.subtotal) + '</td></tr>';
        }).join('');
        var descuento = parseFloat(d.descuento || 0) > 0
            ? '<tr><td>Descuento (cupón)</td><td>-' + pesos(d.descuento) + '</td></tr>' : '';
        var domicilio = d.costo_domicilio != null
            ? '<p class="fd-nota">Domicilio: ' + pesos(d.costo_domicilio) + ' (se paga al recibir, no está incluido en el total).</p>' : '';

        return ''
            + '<div class="fd-cabecera">'
            + '  <div><div class="fd-marca">FlanBross</div><div class="fd-sub">Pastelería · Cartagena de Indias</div></div>'
            + '  <div style="text-align:right"><div class="fd-numero">' + esc(f.numero_factura) + '</div>'
            + '  <div class="fd-sub">' + fecha(f.fecha_factura) + '</div>'
            + '  <div class="fd-sub">Estado: ' + esc(f.estado_factura) + '</div></div>'
            + '</div>'
            + '<div class="fd-bloque"><strong>Cliente:</strong> ' + esc(f.nombre) + ' ' + esc(f.apellido || '')
            + ' · ' + esc(f.correo) + '<br><strong>Pedido:</strong> #' + f.id_pedido
            + (d.direccion_entrega ? '<br><strong>Entrega en:</strong> ' + esc(d.direccion_entrega) : '')
            + (d.metodo_pago ? '<br><strong>Pago:</strong> ' + esc(METODOS[d.metodo_pago] || d.metodo_pago)
                + (d.estado_pago ? ' (' + esc(d.estado_pago) + ')' : '') : '')
            + '</div>'
            + '<table class="fd-tabla"><thead><tr><th>Producto</th><th style="text-align:center">Cant.</th>'
            + '<th style="text-align:right">Precio</th><th style="text-align:right">Subtotal</th></tr></thead>'
            + '<tbody>' + filas + '</tbody></table>'
            + '<table class="fd-totales">'
            + '<tr><td>Subtotal productos</td><td>' + pesos(d.subtotal_productos) + '</td></tr>'
            + descuento
            + '<tr><td>Base</td><td>' + pesos(f.subtotal) + '</td></tr>'
            + '<tr><td>IVA (8 %)</td><td>' + pesos(f.impuestos) + '</td></tr>'
            + '<tr class="fd-total"><td>Total factura</td><td>' + pesos(f.total_factura) + '</td></tr>'
            + '</table>'
            + domicilio
            + (d.observaciones ? '<p class="fd-nota">Observaciones: ' + esc(d.observaciones) + '</p>' : '');
    }

    var ESTILOS = ''
        + '.fd-cabecera{display:flex;justify-content:space-between;gap:16px;border-bottom:2px solid #6b3e26;padding-bottom:12px;margin-bottom:14px}'
        + '.fd-marca{font-size:24px;font-weight:700;color:#6b3e26;font-family:Georgia,serif}'
        + '.fd-numero{font-size:18px;font-weight:700;color:#6b3e26}'
        + '.fd-sub{font-size:12px;color:#8a7365}'
        + '.fd-bloque{font-size:13px;line-height:1.6;margin-bottom:14px}'
        + '.fd-tabla{width:100%;border-collapse:collapse;font-size:13px;margin-bottom:12px}'
        + '.fd-tabla th{background:#f7f1ea;padding:8px;text-align:left;font-size:12px}'
        + '.fd-tabla td{padding:8px;border-bottom:1px solid #eee}'
        + '.fd-totales{margin-left:auto;font-size:13px;border-collapse:collapse;min-width:260px}'
        + '.fd-totales td{padding:4px 8px}.fd-totales td:last-child{text-align:right}'
        + '.fd-total td{font-weight:700;font-size:15px;border-top:2px solid #6b3e26;padding-top:8px}'
        + '.fd-nota{font-size:12px;color:#8a7365;margin-top:10px}';

    function asegurarModal() {
        var m = document.getElementById('modal-factura-detalle');
        if (m) return m;
        var estilo = document.createElement('style');
        estilo.textContent = ESTILOS;
        document.head.appendChild(estilo);

        m = document.createElement('div');
        m.id = 'modal-factura-detalle';
        m.style.cssText = 'display:none;position:fixed;inset:0;background:rgba(0,0,0,0.55);z-index:1000;'
            + 'align-items:center;justify-content:center;padding:16px';
        m.innerHTML = '<div style="background:white;border-radius:16px;padding:28px;width:640px;max-width:100%;'
            + 'max-height:90vh;overflow-y:auto;font-family:inherit;color:#3b2418">'
            + '<div id="fd-contenido">Cargando…</div>'
            + '<div style="display:flex;gap:10px;margin-top:18px">'
            + '<button type="button" class="btn-principal" style="flex:1;justify-content:center" id="fd-imprimir">Imprimir / guardar PDF</button>'
            + '<button type="button" class="btn-secundario" style="flex:1;justify-content:center" id="fd-cerrar">Cerrar</button>'
            + '</div></div>';
        document.body.appendChild(m);
        m.addEventListener('click', function (e) { if (e.target === m) m.style.display = 'none'; });
        document.getElementById('fd-cerrar').onclick = function () { m.style.display = 'none'; };
        document.getElementById('fd-imprimir').onclick = function () {
            var v = window.open('', '_blank');
            if (!v) return;
            v.document.write('<!DOCTYPE html><html><head><meta charset="utf-8"><title>Factura FlanBross</title>'
                + '<style>body{font-family:Arial,sans-serif;color:#3b2418;padding:24px;max-width:720px;margin:auto}'
                + ESTILOS + '</style></head><body>' + document.getElementById('fd-contenido').innerHTML
                + '</body></html>');
            v.document.close();
            v.focus();
            v.print();
        };
        return m;
    }

    // Abre la factura del pedido indicado.
    window.verFactura = function (idPedido) {
        var m = asegurarModal();
        var cont = document.getElementById('fd-contenido');
        cont.textContent = 'Cargando…';
        m.style.display = 'flex';
        fetch('/api/facturas/pedido/' + idPedido)
            .then(function (r) { return r.json(); })
            .then(function (resp) {
                if (!resp.exito) { cont.textContent = resp.mensaje || 'No se pudo cargar la factura.'; return; }
                cont.innerHTML = htmlFactura(resp.datos);
            })
            .catch(function () { cont.textContent = 'No se pudo conectar con el servidor.'; });
    };
})();
