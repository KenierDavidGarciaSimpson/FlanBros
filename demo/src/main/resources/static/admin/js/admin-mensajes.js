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

var mensajesAdmin = [];

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
            mensajesAdmin = resp.datos || [];
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
                    + '<td style="display:flex;gap:6px;flex-wrap:wrap">'
                    + '<button class="btn-sm btn-sm-principal" onclick="abrirResponder(' + m.id_mensaje + ')">Responder</button>'
                    + (leido ? '' : '<button class="btn-sm btn-sm-borde" onclick="marcarMensajeLeido(' + m.id_mensaje + ')">Marcar leído</button>')
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

// ---- Responder un mensaje por correo ----
function abrirResponder(id) {
    var m = mensajesAdmin.find(function(x) { return x.id_mensaje === id; });
    if (!m) return;
    document.getElementById('mr-id').value = id;
    document.getElementById('mr-para').textContent = 'Para: ' + m.nombre + ' <' + m.correo + '>';
    document.getElementById('mr-original').textContent = m.mensaje;
    document.getElementById('mr-texto').value = '';
    document.getElementById('modal-responder').style.display = 'flex';
    document.getElementById('mr-texto').focus();
}

function cerrarResponder() { document.getElementById('modal-responder').style.display = 'none'; }

function enviarRespuesta() {
    var id = document.getElementById('mr-id').value;
    var texto = document.getElementById('mr-texto').value.trim();
    if (texto.length < 5) { mostrarToast('La respuesta debe tener al menos 5 caracteres', 'error'); return; }

    var boton = document.getElementById('mr-enviar');
    boton.disabled = true;
    boton.textContent = 'Enviando…';
    fetch('/api/contacto/' + id + '/respuesta', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ respuesta: texto })
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        mostrarToast(resp.mensaje || (resp.exito ? 'Respuesta enviada' : 'No se pudo enviar'),
                     resp.exito ? 'ok' : 'error');
        if (resp.exito) { cerrarResponder(); cargarMensajesAdmin(); }
    })
    .catch(function() { mostrarToast('No se pudo conectar con el servidor', 'error'); })
    .finally(function() { boton.disabled = false; boton.textContent = 'Enviar respuesta'; });
}
