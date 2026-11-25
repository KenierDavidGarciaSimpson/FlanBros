// admin-empleados.js — Listado, creación y desactivación de empleados

function cargarEmpleadosAdmin() {
    fetch('/api/empleados')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-empleados');
            if (!el || !resp.exito) return;
            el.innerHTML = resp.datos.map(function(e) {
                return '<tr>'
                    + '<td>' + e.nombre + ' ' + (e.apellido || '') + '</td>'
                    + '<td>' + (e.correo || '—') + '</td>'
                    + '<td>' + e.nombre_rol + '</td>'
                    + '<td>' + formatearPrecio(e.salario) + '</td>'
                    + '<td><span class="estado-badge">' + e.estado_empleado + '</span></td>'
                    + '<td style="display:flex;gap:6px">'
                    + '<button class="btn-sm btn-sm-borde" onclick=\'abrirEditarEmpleado(' + JSON.stringify(e) + ')\'>Editar</button>'
                    + '<button class="btn-sm" style="background:#c0392b;color:white;border:none" onclick="desactivarEmpleado(' + e.id_empleado + ')">Desactivar</button>'
                    + '</td>'
                    + '</tr>';
            }).join('');
        });
}

// Abre el modal (null = nuevo empleado, objeto = editar)
function abrirModalEmpleado(e) {
    var m = document.getElementById('modal-empleado');
    if (!m) return;
    document.getElementById('me-id').value          = e ? e.id_empleado   : '';
    document.getElementById('me-nombre').value      = e ? e.nombre        : '';
    document.getElementById('me-apellido').value    = e ? (e.apellido||'') : '';
    document.getElementById('me-documento').value   = e ? (e.documento||'') : '';
    document.getElementById('me-telefono').value    = e ? (e.telefono||'') : '';
    document.getElementById('me-correo').value      = e ? (e.correo||'')  : '';
    document.getElementById('me-salario').value     = e ? e.salario       : '';
    document.getElementById('me-fecha').value       = e ? (e.fecha_contratacion||'') : '';
    document.getElementById('me-rol').value         = e ? e.id_rol        : '2';
    m.style.display = 'flex';
}

function abrirNuevoEmpleado()   { abrirModalEmpleado(null); }
function abrirEditarEmpleado(e) { abrirModalEmpleado(e); }
function cerrarModalEmpleado()  { document.getElementById('modal-empleado').style.display = 'none'; }

function guardarEmpleado() {
    var id = document.getElementById('me-id').value;
    var datos = {
        nombre:              document.getElementById('me-nombre').value,
        apellido:            document.getElementById('me-apellido').value,
        documento:           document.getElementById('me-documento').value,
        telefono:            document.getElementById('me-telefono').value,
        correo:              document.getElementById('me-correo').value,
        salario:             parseFloat(document.getElementById('me-salario').value) || 0,
        fecha_contratacion:  document.getElementById('me-fecha').value,
        id_rol:              parseInt(document.getElementById('me-rol').value)
    };
    if (!datos.nombre) { mostrarToast('El nombre es obligatorio', 'error'); return; }
    if (id) { datos.id_empleado = parseInt(id); datos.estado_empleado = 'activo'; }

    fetch('/api/empleados', {
        method: id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(datos)
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        if (r.exito) {
            cerrarModalEmpleado();
            cargarEmpleadosAdmin();
            mostrarToast(id ? 'Empleado actualizado' : 'Empleado creado');
        } else {
            mostrarToast(r.mensaje, 'error');
        }
    });
}

function desactivarEmpleado(id) {
    if (!confirm('¿Desactivar este empleado?')) return;
    fetch('/api/empleados', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_empleado: id })
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        mostrarToast(r.exito ? 'Empleado desactivado' : r.mensaje, r.exito ? 'ok' : 'error');
        if (r.exito) cargarEmpleadosAdmin();
    });
}
