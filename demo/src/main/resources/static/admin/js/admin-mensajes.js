// admin-mensajes.js — Bandeja de mensajes del formulario de contacto

// Escapa el texto del usuario antes de insertarlo como HTML.
function _escaparHtml(texto) {
    return String(texto == null ? '' : texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function cargarMensajesAdmin() {
    fetch('/api/contacto')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-mensajes');
            if (!el) return;
            if (!resp.exito) {
                el.innerHTML = '<tr><td colspan="7" style="padding:16px;color:#c0392b">No se pudieron cargar los mensajes.</td></tr>';
                return;
            }
            var lista = (resp.datos || []).slice().sort(function(a, b) {
                return (b.id_mensaje || 0) - (a.id_mensaje || 0);
            });
            if (lista.length === 0) {
                el.innerHTML = '<tr><td colspan="7" style="padding:16px;color:var(--texto-claro)">No hay mensajes.</td></tr>';
                return;
            }
            el.innerHTML = lista.map(function(m) {
                var leido = m.estado === 'leido';
                var fecha = m.fecha_envio ? formatearFecha(m.fecha_envio) : '—';
                return '<tr>'
                    + '<td><span class="estado-badge"' + (leido ? ' style="opacity:.6"' : ' style="background:var(--caramelo);color:white"')
                    + '>' + (leido ? 'Leído' : 'Sin leer') + '</span></td>'
                    + '<td>' + _escaparHtml(m.nombre) + '</td>'
                    + '<td>' + _escaparHtml(m.correo) + '</td>'
                    + '<td>' + _escaparHtml(m.telefono || '—') + '</td>'
                    + '<td style="max-width:340px;white-space:pre-wrap;word-break:break-word">' + _escaparHtml(m.mensaje) + '</td>'
                    + '<td>' + fecha + '</td>'
                    + '<td>' + (leido
                        ? '<span style="font-size:12px;color:var(--texto-claro)">—</span>'
                        : '<button class="btn-sm btn-sm-principal" onclick="marcarMensajeLeido(' + m.id_mensaje + ')">Marcar leído</button>')
                    + '</td>'
                    + '</tr>';
            }).join('');
        })
        .catch(function() {
            var el = document.getElementById('tabla-mensajes');
            if (el) el.innerHTML = '<tr><td colspan="7" style="padding:16px;color:#c0392b">No se pudo conectar con el servidor.</td></tr>';
        });
}

function marcarMensajeLeido(id) {
    fetch('/api/contacto', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_mensaje: id })
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        mostrarToast(resp.exito ? 'Mensaje marcado como leído'
                                : (resp.mensaje || 'No se pudo actualizar'),
                     resp.exito ? 'ok' : 'error');
        if (resp.exito) cargarMensajesAdmin();
    })
    .catch(function() {
        mostrarToast('No se pudo conectar con el servidor', 'error');
    });
}
