// admin-clientes.js — Listado y activación/desactivación de clientes

var clientesAdmin = [];

function cargarClientesAdmin() {
    fetch('/api/clientes')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-clientes');
            if (!el || !resp.exito) return;
            clientesAdmin = resp.datos;
            el.innerHTML = resp.datos.map(function(c) {
                return '<tr>'
                    + '<td>' + c.nombre + ' ' + (c.apellido || '') + '</td>'
                    + '<td>' + c.correo + '</td>'
                    + '<td>' + (c.telefono || '—') + '</td>'
                    + '<td>' + (c.total_pedidos || 0) + '</td>'
                    + '<td><span class="estado-badge">' + c.estado_cliente + '</span></td>'
                    + '<td style="display:flex;gap:6px"><button class="btn-sm btn-sm-borde" onclick="toggleCliente(' + c.id_cliente + ',\'' + c.estado_cliente + '\')">'
                    + (c.estado_cliente === 'activo' ? 'Desactivar' : 'Activar') + '</button>'
                    + (c.estado_cliente === 'activo'
                        ? '<button class="btn-sm btn-sm-principal" onclick="abrirConvertir(' + c.id_cliente + ')">Pasar a empleado</button>'
                        : '')
                    + '</td>'
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

// ---- Pasar un cliente a empleado ----
function abrirConvertir(id) {
    var c = clientesAdmin.find(function(x) { return x.id_cliente === id; });
    if (!c) return;
    document.getElementById('mc-id').value = id;
    document.getElementById('mc-nombre').textContent = c.nombre + ' ' + (c.apellido || '') + ' · ' + c.correo;
    document.getElementById('mc-rol').value = '3';
    document.getElementById('mc-salario').value = '';
    document.getElementById('mc-documento').value = '';
    document.getElementById('modal-convertir').style.display = 'flex';
}

function cerrarConvertir() { document.getElementById('modal-convertir').style.display = 'none'; }

function confirmarConvertir() {
    var id = parseInt(document.getElementById('mc-id').value);
    var rol = document.getElementById('mc-rol');
    var salario = parseFloat(document.getElementById('mc-salario').value) || 0;
    if (salario < 0) { mostrarToast('El salario no puede ser negativo', 'error'); return; }
    if (!confirm('¿Pasar este cliente a ' + rol.options[rol.selectedIndex].text + '?')) return;

    fetch('/api/clientes/' + id + '/empleado', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            id_rol: parseInt(rol.value),
            salario: salario,
            documento: document.getElementById('mc-documento').value.trim()
        })
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        mostrarToast(r.mensaje || (r.exito ? 'Listo' : 'No se pudo'), r.exito ? 'ok' : 'error');
        if (r.exito) {
            cerrarConvertir();
            cargarClientesAdmin();
            if (typeof cargarEmpleadosAdmin === 'function') cargarEmpleadosAdmin();
        }
    });
}
