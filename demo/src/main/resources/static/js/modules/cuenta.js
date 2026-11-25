// cuenta.js — Perfil e historial de pedidos del cliente

function cargarMiCuenta() {
    const usuario = obtenerSesion();
    if (!usuario) { window.location.href = 'login.html'; return; }

    const elNombre = document.getElementById('nombre-usuario');
    const elPerfil = document.getElementById('nombre-perfil');
    const elCorreo = document.getElementById('correo-perfil');
    const elAvatar = document.getElementById('avatar-iniciales');

    if (elNombre) elNombre.textContent = usuario.nombre;
    if (elPerfil) elPerfil.textContent = usuario.nombre + ' ' + (usuario.apellido || '');
    if (elCorreo) elCorreo.textContent = usuario.correo;
    if (elAvatar) elAvatar.textContent = ((usuario.nombre || '')[0] + (usuario.apellido || '')[0]).toUpperCase();

    // Banner de confirmación si viene de hacer un pedido
    const factura = localStorage.getItem('fb_pedido_confirmado');
    if (factura) {
        const banner = document.getElementById('banner-exito');
        const numFac = document.getElementById('numero-factura-confirmada');
        if (banner) banner.style.display = 'flex';
        if (numFac) numFac.textContent = factura;
        localStorage.removeItem('fb_pedido_confirmado');
    }

    fetch(API + '/pedidos?id_cliente=' + usuario.id_entidad)
        .then(function(r) { return r.json(); })
        .then(function(resp) {
            if (resp.exito) renderizarPedidosCliente(resp.datos);
        })
        .catch(function() {});
}

function renderizarPedidosCliente(pedidos) {
    const contenedor = document.getElementById('historial-pedidos');
    if (!contenedor) return;

    const elTotal = document.getElementById('stat-pedidos');
    const elGasto = document.getElementById('stat-gasto');
    if (elTotal) elTotal.textContent = pedidos.length;
    if (elGasto) {
        const suma = pedidos.reduce(function(acc, p) { return acc + parseFloat(p.total_pedido); }, 0);
        elGasto.textContent = formatearPrecio(suma);
    }

    if (pedidos.length === 0) {
        contenedor.innerHTML = '<p style="padding:20px;color:var(--texto-claro)">Aún no tienes pedidos. <a href="catalogo.html" style="color:var(--caramelo)">Ver catálogo</a></p>';
        return;
    }

    // Pedido activo = el más reciente que no esté entregado ni cancelado
    const activo   = pedidos.find(function(p) { return p.estado_pedido !== 'entregado' && p.estado_pedido !== 'cancelado'; });
    const elActivo = document.getElementById('pedido-activo');
    if (elActivo && activo) {
        elActivo.style.display = 'block';
        const elFac = document.getElementById('factura-activa');
        const elEst = document.getElementById('estado-activo');
        if (elFac) elFac.textContent = activo.numero_factura || 'Pedido #' + activo.id_pedido;
        if (elEst) elEst.textContent = etiquetaEstado(activo.estado_pedido);
    }

    contenedor.innerHTML = pedidos.map(function(p) {
        return '<div class="item-pedido">'
            + '<div class="item-pedido-info">'
            + '<div class="item-pedido-nombre">' + (p.numero_factura || 'Pedido #' + p.id_pedido) + '</div>'
            + '<div class="item-pedido-fecha">' + formatearFecha(p.fecha_pedido) + '</div>'
            + '<span class="estado-badge estado-' + p.estado_pedido.replace('_','-') + '">' + etiquetaEstado(p.estado_pedido) + '</span>'
            + '</div>'
            + '<div class="item-pedido-precio">' + formatearPrecio(p.total_pedido) + '</div>'
            + '</div>';
    }).join('');
}
