var TASA_IVA        = 0.08;
var COSTO_DOMICILIO = 7000;

function claveCarrito() {
    var u = obtenerSesion();
    return u ? 'fb_carrito_' + (u.id_usuario || u.id_entidad || u.correo) : null;
}

function obtenerCarrito() {
    var clave = claveCarrito();
    if (!clave) return [];
    try {
        var lista = JSON.parse(localStorage.getItem(clave)) || [];
        return Array.isArray(lista) ? lista.filter(function (i) { return i && i.id_producto > 0 && i.cantidad > 0; }) : [];
    } catch (e) { return []; }
}

function guardarCarrito(carrito) {
    var clave = claveCarrito();
    if (!clave) return;
    localStorage.setItem(clave, JSON.stringify(carrito));
}

function cantidadEnCarrito(idProducto) {
    var item = obtenerCarrito().find(function (i) { return i.id_producto === idProducto; });
    return item ? item.cantidad : 0;
}

function escaparHTML(texto) {
    return String(texto == null ? '' : texto)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

var _promesaProductos = null;

function obtenerProductos(forzar) {
    if (window.PRODUCTOS && !forzar) return Promise.resolve(window.PRODUCTOS);
    if (_promesaProductos && !forzar) return _promesaProductos;
    _promesaProductos = fetch(API + '/productos')
        .then(function (r) { return r.json(); })
        .then(function (resp) {
            if (!resp.exito) throw new Error(resp.mensaje || 'No se pudieron cargar los productos');
            window.PRODUCTOS = resp.datos || [];
            return window.PRODUCTOS;
        })
        .catch(function (e) { _promesaProductos = null; throw e; });
    return _promesaProductos;
}

function buscarProducto(id) {
    return obtenerProductos().then(function (lista) {
        return lista.find(function (p) { return p.id_producto === id; }) || null;
    });
}

function idDe(productoOId) {
    if (typeof productoOId === 'number') return productoOId;
    if (typeof productoOId === 'string') return parseInt(productoOId, 10) || 0;
    if (productoOId) return productoOId.id_producto || productoOId.id || 0;
    return 0;
}

function estaDisponible(p) {
    return p && p.stock > 0 && (p.estado_producto || 'disponible') === 'disponible';
}

function exigirSesionParaComprar() {
    var u = obtenerSesion();
    if (u && u.nombre_rol && u.nombre_rol !== 'cliente') {
        mostrarToast('Las cuentas del personal no pueden hacer pedidos. Usa una cuenta de cliente.', 'error');
        return false;
    }
    if (u) return true;
    try { sessionStorage.setItem('fb_volver', window.location.pathname + window.location.search); } catch (e) {  }
    mostrarToast('Inicia sesión para agregar productos al carrito', 'error');
    setTimeout(function () { window.location.href = '/login.html'; }, 900);
    return false;
}

function agregarAlCarrito(productoOId) {
    if (!exigirSesionParaComprar()) return;
    var id = idDe(productoOId);
    if (!id) { mostrarToast('Producto no válido', 'error'); return; }
    buscarProducto(id)
        .then(function (p) {
            if (!p) { mostrarToast('Ese producto ya no está en el catálogo', 'error'); return; }
            if (!estaDisponible(p)) { mostrarToast(p.nombre_producto + ' está agotado', 'error'); return; }
            var libre = p.stock - cantidadEnCarrito(p.id_producto);
            if (libre <= 0) {
                mostrarToast('Ya tienes en el carrito todas las unidades disponibles de ' + p.nombre_producto, 'error');
                return;
            }
            mostrarModalCantidad(p);
        })
        .catch(function () { mostrarToast('No se pudo conectar con el servidor', 'error'); });
}

function agregarCantidadAlCarrito(p, cant) {
    if (!exigirSesionParaComprar()) return false;
    cant = parseInt(cant, 10) || 0;
    if (!p || cant < 1) { mostrarToast('Elige una cantidad válida', 'error'); return false; }
    if (!estaDisponible(p)) { mostrarToast(p.nombre_producto + ' está agotado', 'error'); return false; }

    var carrito = obtenerCarrito();
    var pos = carrito.findIndex(function (i) { return i.id_producto === p.id_producto; });
    var yaTiene = pos >= 0 ? carrito[pos].cantidad : 0;
    if (yaTiene + cant > p.stock) {
        var libre = p.stock - yaTiene;
        mostrarToast(libre > 0
            ? 'Solo puedes agregar ' + libre + ' unidad(es) más de ' + p.nombre_producto
            : 'Ya tienes en el carrito todas las unidades disponibles', 'error');
        return false;
    }

    if (pos >= 0) {
        carrito[pos].cantidad += cant;
        carrito[pos].precio = Number(p.precio);
        carrito[pos].stock = p.stock;
    } else {
        carrito.push({
            id_producto: p.id_producto,
            nombre:      p.nombre_producto,
            precio:      Number(p.precio),
            imagen:      p.imagen || null,
            emoji:       p.emoji || '🍰',
            categoria:   p.categoria || '',
            stock:       p.stock,
            cantidad:    cant
        });
    }
    guardarCarrito(carrito);
    actualizarBadgeCarrito();
    mostrarToast(cant + ' × ' + p.nombre_producto + ' agregado al carrito');
    return true;
}

var _productoModal = null;
var _modalTimeout  = null;

function maximoParaModal() {
    if (!_productoModal) return 1;
    return Math.max(1, _productoModal.stock - cantidadEnCarrito(_productoModal.id_producto));
}

function mostrarModalCantidad(producto) {
    if (_modalTimeout) { clearTimeout(_modalTimeout); _modalTimeout = null; }
    _productoModal = producto;

    var modal = document.getElementById('modal-cantidad-carrito');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'modal-cantidad-carrito';
        modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.45);z-index:9000;display:flex;align-items:center;justify-content:center;padding:20px';
        modal.innerHTML =
            '<div style="background:white;border-radius:18px;padding:28px 28px 22px;width:100%;max-width:340px;box-shadow:0 8px 32px rgba(61,28,2,.18)">'
          + '<div style="width:100%;height:140px;background:var(--crema-2);border-radius:12px;overflow:hidden;display:flex;align-items:center;justify-content:center;margin-bottom:16px">'
          + '<img id="mc-img" alt="" style="width:100%;height:100%;object-fit:cover;display:none">'
          + '<span id="mc-emoji" style="font-size:52px;display:none"></span></div>'
          + '<div id="mc-categoria" style="font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.1em;color:var(--texto-claro);margin-bottom:4px"></div>'
          + '<div id="mc-nombre" style="font-family:var(--fuente-titulo);font-size:22px;color:var(--chocolate);margin-bottom:4px"></div>'
          + '<div id="mc-precio" style="font-family:var(--fuente-titulo);font-size:26px;color:var(--caramelo);font-weight:700;margin-bottom:18px"></div>'
          + '<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:18px">'
          + '<span style="font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:var(--texto-claro)">Cantidad</span>'
          + '<div style="display:flex;align-items:center;border:1.5px solid var(--borde);border-radius:10px;overflow:hidden">'
          + '<button type="button" onclick="cambiarCantidadModal(-1)" style="width:40px;height:40px;background:none;border:none;font-size:22px;cursor:pointer;color:var(--chocolate)">−</button>'
          + '<span id="mc-cant" style="min-width:44px;text-align:center;font-family:var(--fuente-titulo);font-size:20px;font-weight:700;color:var(--chocolate)">1</span>'
          + '<button type="button" onclick="cambiarCantidadModal(1)" style="width:40px;height:40px;background:none;border:none;font-size:22px;cursor:pointer;color:var(--chocolate)">+</button>'
          + '</div></div>'
          + '<div id="mc-stock-info" style="font-size:12px;color:var(--texto-claro);text-align:right;margin-bottom:14px"></div>'
          + '<button type="button" id="mc-btn-agregar" onclick="confirmarAgregarCarrito()" style="width:100%;padding:15px;background:var(--caramelo);color:white;border:none;border-radius:999px;font-family:var(--fuente-cuerpo);font-size:15px;font-weight:700;cursor:pointer">Agregar al carrito</button>'
          + '<button type="button" onclick="cerrarModalCantidad()" style="width:100%;padding:10px;background:none;border:none;font-size:13px;color:var(--texto-claro);cursor:pointer;margin-top:6px">Cancelar</button>'
          + '</div>';
        document.body.appendChild(modal);
        modal.addEventListener('click', function (e) { if (e.target === modal) cerrarModalCantidad(); });
    }

    var img   = document.getElementById('mc-img');
    var emoji = document.getElementById('mc-emoji');
    emoji.textContent = producto.emoji || '🍰';
    if (producto.imagen) {
        img.onerror = function () { img.style.display = 'none'; emoji.style.display = 'block'; };
        img.src = '/img/productos/' + producto.imagen;
        img.style.display = 'block';
        emoji.style.display = 'none';
    } else {
        img.onerror = null;
        img.removeAttribute('src');
        img.style.display = 'none';
        emoji.style.display = 'block';
    }

    var libre = maximoParaModal();
    var enCarrito = cantidadEnCarrito(producto.id_producto);
    document.getElementById('mc-categoria').textContent  = producto.categoria || '';
    document.getElementById('mc-nombre').textContent     = producto.nombre_producto;
    document.getElementById('mc-cant').textContent       = 1;
    document.getElementById('mc-stock-info').textContent =
        (libre <= 5 ? '⚠ Solo ' + libre + ' disponibles' : libre + ' disponibles')
        + (enCarrito > 0 ? ' · ya tienes ' + enCarrito + ' en el carrito' : '');
    actualizarPrecioModal(1, producto.precio);

    var btn = document.getElementById('mc-btn-agregar');
    btn.disabled = false;
    btn.style.background = 'var(--caramelo)';
    btn.textContent = 'Agregar al carrito';

    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function actualizarPrecioModal(cant, precio) {
    var el = document.getElementById('mc-precio');
    if (el) el.textContent = formatearPrecio(precio * cant);
}

function cambiarCantidadModal(delta) {
    if (!_productoModal) return;
    var el = document.getElementById('mc-cant');
    var maximo = maximoParaModal();
    var actual = parseInt(el.textContent, 10) || 1;
    var nuevo  = actual + delta;
    if (nuevo > maximo) { mostrarToast('Solo hay ' + maximo + ' unidad(es) disponibles', 'error'); nuevo = maximo; }
    if (nuevo < 1) nuevo = 1;
    el.textContent = nuevo;
    actualizarPrecioModal(nuevo, _productoModal.precio);
}

function confirmarAgregarCarrito() {
    if (!_productoModal) return;
    var cant = parseInt(document.getElementById('mc-cant').textContent, 10) || 1;
    if (!agregarCantidadAlCarrito(_productoModal, cant)) return;

    var btn = document.getElementById('mc-btn-agregar');
    btn.disabled = true;
    btn.style.background = '#2d7a5f';
    btn.textContent = '✓ Agregado (' + cant + ')';
    _modalTimeout = setTimeout(cerrarModalCantidad, 800);
}

function cerrarModalCantidad() {
    if (_modalTimeout) { clearTimeout(_modalTimeout); _modalTimeout = null; }
    var modal = document.getElementById('modal-cantidad-carrito');
    if (modal) modal.style.display = 'none';
    document.body.style.overflow = '';
    _productoModal = null;
}

function cambiarCantidad(idProducto, nuevaCantidad) {
    if (nuevaCantidad <= 0) { eliminarDelCarrito(idProducto); return; }
    var carrito = obtenerCarrito();
    var pos = carrito.findIndex(function (i) { return i.id_producto === idProducto; });
    if (pos < 0) return;
    var stock = carrito[pos].stock;
    if (stock && nuevaCantidad > stock) {
        mostrarToast('Solo hay ' + stock + ' unidad(es) disponibles de ' + carrito[pos].nombre, 'error');
        return;
    }
    carrito[pos].cantidad = nuevaCantidad;
    guardarCarrito(carrito);
    renderizarCarrito();
    actualizarBadgeCarrito();
}

function eliminarDelCarrito(idProducto) {
    guardarCarrito(obtenerCarrito().filter(function (i) { return i.id_producto !== idProducto; }));
    renderizarCarrito();
    actualizarBadgeCarrito();
}

function vaciarCarrito() {
    guardarCarrito([]);
    try { localStorage.removeItem('fb_cupon'); localStorage.removeItem('fb_cupon_data'); } catch (e) {  }
    if (typeof quitarCupon === 'function') quitarCupon(true);
    renderizarCarrito();
    actualizarBadgeCarrito();
}

function sincronizarCarrito() {
    var carrito = obtenerCarrito();
    if (carrito.length === 0) return Promise.resolve(false);
    return obtenerProductos(true).then(function (lista) {
        var avisos = [];
        var nuevo = [];
        carrito.forEach(function (item) {
            var p = lista.find(function (x) { return x.id_producto === item.id_producto; });
            if (!p || !estaDisponible(p)) { avisos.push(item.nombre + ' ya no está disponible y se quitó del carrito'); return; }
            if (item.cantidad > p.stock) {
                avisos.push('De ' + p.nombre_producto + ' solo quedan ' + p.stock + '; se ajustó la cantidad');
                item.cantidad = p.stock;
            }
            item.precio = Number(p.precio);
            item.stock  = p.stock;
            item.nombre = p.nombre_producto;
            nuevo.push(item);
        });
        guardarCarrito(nuevo);
        avisos.forEach(function (a) { mostrarToast(a, 'error'); });
        return avisos.length > 0;
    }).catch(function () { return false; });
}

function calcularSubtotal() {
    return obtenerCarrito().reduce(function (acc, i) { return acc + i.precio * i.cantidad; }, 0);
}

function calcularTotales() {
    var subtotal  = calcularSubtotal();
    var descuento = (typeof obtenerDescuento === 'function') ? obtenerDescuento() : 0;
    var base      = Math.max(subtotal - descuento, 0);
    var iva       = Math.round(base * TASA_IVA);
    return { subtotal: subtotal, descuento: descuento, iva: iva, total: base + iva, domicilio: COSTO_DOMICILIO };
}

function actualizarBadgeCarrito() {
    var total = obtenerCarrito().reduce(function (acc, i) { return acc + i.cantidad; }, 0);
    document.querySelectorAll('.carrito-badge').forEach(function (badge) {
        badge.textContent = total;
        badge.style.display = total > 0 ? 'flex' : 'none';
    });
}

function renderizarCarrito() {
    var contenedor = document.getElementById('lista-carrito');
    if (!contenedor) return;
    var carrito = obtenerCarrito();
    var btnPago = document.getElementById('btn-ir-pago');

    if (carrito.length === 0) {
        contenedor.innerHTML = '<p style="padding:32px;text-align:center;color:var(--texto-claro)">Tu carrito está vacío. <a href="catalogo.html" style="color:var(--caramelo)">Ver catálogo</a></p>';
        if (btnPago) { btnPago.style.opacity = '.5'; btnPago.style.pointerEvents = 'none'; }
        actualizarResumenCarrito();
        return;
    }
    if (btnPago) { btnPago.style.opacity = ''; btnPago.style.pointerEvents = ''; }

    contenedor.innerHTML = carrito.map(function (item) {
        var nombre = escaparHTML(item.nombre);
        var imgHTML = item.imagen
            ? '<img src="img/productos/' + escaparHTML(item.imagen) + '" alt="' + nombre + '" style="width:60px;height:60px;object-fit:cover;border-radius:10px" onerror="this.style.display=\'none\'">'
            : '<div style="width:60px;height:60px;background:var(--crema-2);border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:28px">' + escaparHTML(item.emoji || '🍰') + '</div>';
        var tope = item.stock && item.cantidad >= item.stock;

        return '<div style="display:flex;align-items:center;gap:16px;padding:18px 0;border-bottom:1px solid var(--borde)">'
            + imgHTML
            + '<div style="flex:1">'
            + '<div style="font-weight:600;color:var(--chocolate);margin-bottom:4px">' + nombre + '</div>'
            + '<div style="font-size:13px;color:var(--texto-claro)">' + escaparHTML(item.categoria) + ' · ' + formatearPrecio(item.precio) + ' c/u'
            + (tope ? ' · <span style="color:#c0392b">máximo disponible</span>' : '') + '</div>'
            + '</div>'
            + '<div style="display:flex;align-items:center;gap:8px">'
            + '<button type="button" aria-label="Quitar uno" onclick="cambiarCantidad(' + item.id_producto + ',' + (item.cantidad - 1) + ')" style="width:28px;height:28px;border-radius:50%;border:1px solid var(--borde);background:white;cursor:pointer;font-size:16px">−</button>'
            + '<span style="font-weight:600;min-width:20px;text-align:center">' + item.cantidad + '</span>'
            + '<button type="button" aria-label="Agregar uno" onclick="cambiarCantidad(' + item.id_producto + ',' + (item.cantidad + 1) + ')" style="width:28px;height:28px;border-radius:50%;border:1px solid var(--borde);background:white;cursor:pointer;font-size:16px' + (tope ? ';opacity:.4' : '') + '">+</button>'
            + '</div>'
            + '<div style="min-width:90px;text-align:right">'
            + '<div style="font-family:var(--fuente-titulo);font-size:17px;color:var(--caramelo)">' + formatearPrecio(item.precio * item.cantidad) + '</div>'
            + '<button type="button" onclick="eliminarDelCarrito(' + item.id_producto + ')" style="font-size:11px;color:var(--texto-claro);background:none;border:none;cursor:pointer;margin-top:4px">Eliminar</button>'
            + '</div>'
            + '</div>';
    }).join('');

    actualizarResumenCarrito();
}

function actualizarResumenCarrito() {
    var t = calcularTotales();
    var poner = function (id, texto) { var el = document.getElementById(id); if (el) el.textContent = texto; };
    poner('resumen-subtotal',  formatearPrecio(t.subtotal));
    poner('resumen-descuento', t.descuento > 0 ? '-' + formatearPrecio(t.descuento) : '—');
    poner('resumen-iva',       formatearPrecio(t.iva));
    poner('resumen-total',     formatearPrecio(t.total));
}

function irAlPago(evento) {
    if (evento) evento.preventDefault();
    if (!exigirSesionParaComprar()) return;
    if (obtenerCarrito().length === 0) { mostrarToast('Tu carrito está vacío', 'error'); return; }
    sincronizarCarrito().then(function (cambio) {
        if (cambio) { renderizarCarrito(); actualizarBadgeCarrito(); mostrarToast('Revisa tu carrito: hubo cambios de disponibilidad', 'error'); return; }
        if (obtenerCarrito().length === 0) { renderizarCarrito(); return; }
        window.location.href = 'checkout.html';
    });
}
