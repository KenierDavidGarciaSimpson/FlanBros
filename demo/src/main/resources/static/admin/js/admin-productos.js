function cargarProductosAdmin() {
    fetch('/api/productos')
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            var el = document.getElementById('tabla-productos');
            if (!el || !resp.exito) return;
            el.innerHTML = resp.datos.map(function(p) {
                return '<tr>'
                    + '<td><img src="../img/productos/' + slugificar(p.nombre_producto) + '.jpg" style="width:38px;height:38px;object-fit:cover;border-radius:7px;margin-right:8px;vertical-align:middle" onerror="this.style.display=\'none\'">' + p.nombre_producto + '</td>'
                    + '<td>' + p.categoria + '</td>'
                    + '<td>' + formatearPrecio(p.precio) + '</td>'
                    + '<td>' + p.stock + '</td>'
                    + '<td><span class="estado-badge">' + p.estado_producto + '</span></td>'
                    + '<td style="display:flex;gap:6px">'
                    + '<button class="btn-sm btn-sm-borde" onclick=\'abrirEditarProducto(' + JSON.stringify(p) + ')\'>Editar</button>'
                    + '<button class="btn-sm" style="background:#c0392b;color:white;border:none" onclick="eliminarProducto(' + p.id_producto + ')">Quitar</button>'
                    + '</td>'
                    + '</tr>';
            }).join('');
        });
}

function abrirModalProducto(p) {
    var m = document.getElementById('modal-producto');
    if (!m) return;
    document.getElementById('mp-id').value          = p ? p.id_producto : '';
    document.getElementById('mp-nombre').value      = p ? p.nombre_producto : '';
    document.getElementById('mp-descripcion').value = p ? p.descripcion : '';
    document.getElementById('mp-precio').value      = p ? p.precio : '';
    document.getElementById('mp-stock').value       = p ? p.stock : '';
    document.getElementById('mp-categoria').value   = p ? p.categoria : '';
    document.getElementById('mp-estado').value      = p ? p.estado_producto : 'disponible';
    document.getElementById('mp-imagen').value      = p ? (p.imagen || '') : '';
    document.getElementById('mp-img-file').value    = '';
    document.getElementById('mp-img-estado').textContent = '';

    var thumb = document.getElementById('mp-img-thumb');
    var holder = document.getElementById('mp-img-placeholder');
    if (p && p.imagen) {
        thumb.src = '/img/productos/' + p.imagen;
        thumb.style.display = 'block';
        holder.style.display = 'none';
    } else {
        thumb.style.display = 'none';
        holder.style.display = 'block';
    }
    m.style.display = 'flex';
}

function previsualizarImagen(input) {
    if (!input.files || !input.files[0]) return;
    var file = input.files[0];
    var estadoEl = document.getElementById('mp-img-estado');
    var thumb    = document.getElementById('mp-img-thumb');
    var holder   = document.getElementById('mp-img-placeholder');

    var reader = new FileReader();
    reader.onload = function(e) {
        thumb.src = e.target.result;
        thumb.style.display = 'block';
        holder.style.display = 'none';
    };
    reader.readAsDataURL(file);

    estadoEl.textContent = 'Subiendo imagen...';
    estadoEl.style.color = 'var(--texto-claro)';

    var formData = new FormData();
    formData.append('imagen', file);

    fetch('/api/productos/imagen', {
        method: 'POST',
        body: formData
    })
    .then(function(r) { return r.json(); })
    .then(function(resp) {
        if (resp.exito) {
            document.getElementById('mp-imagen').value = resp.datos.nombre_archivo;
            estadoEl.textContent = '✓ Imagen subida: ' + resp.datos.nombre_archivo;
            estadoEl.style.color = '#2d7a5f';
        } else {
            estadoEl.textContent = '✗ Error: ' + resp.mensaje;
            estadoEl.style.color = '#c0392b';
            document.getElementById('mp-img-file').value = '';
        }
    })
    .catch(function() {
        estadoEl.textContent = '✗ No se pudo subir la imagen. Verifica que el servidor esté activo.';
        estadoEl.style.color = '#c0392b';
    });
}

function abrirEditarProducto(p) { abrirModalProducto(p); }
function abrirNuevoProducto()   { abrirModalProducto(null); }
function cerrarModalProducto()  { document.getElementById('modal-producto').style.display = 'none'; }

function guardarProducto() {
    var id = document.getElementById('mp-id').value;
    var datos = {
        nombre_producto: document.getElementById('mp-nombre').value,
        descripcion:     document.getElementById('mp-descripcion').value,
        precio:          parseFloat(document.getElementById('mp-precio').value),
        stock:           parseInt(document.getElementById('mp-stock').value),
        categoria:       document.getElementById('mp-categoria').value,
        estado_producto: document.getElementById('mp-estado').value,
        imagen:          document.getElementById('mp-imagen').value
    };
    if (id) datos.id_producto = parseInt(id);

    fetch('/api/productos', {
        method: id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(datos)
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        if (r.exito) {
            cerrarModalProducto();
            cargarProductosAdmin();
            mostrarToast(id ? 'Producto actualizado' : 'Producto creado');
        } else {
            mostrarToast(r.mensaje, 'error');
        }
    });
}

function eliminarProducto(id) {
    if (!confirm('¿Desactivar este producto del catálogo?')) return;
    fetch('/api/productos', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_producto: id })
    })
    .then(function(r) { return r.json(); })
    .then(function(r) {
        mostrarToast(r.exito ? 'Producto desactivado' : r.mensaje, r.exito ? 'ok' : 'error');
        if (r.exito) cargarProductosAdmin();
    });
}
