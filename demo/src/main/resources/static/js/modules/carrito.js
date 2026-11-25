// carrito.js — Lógica del carrito de compras

const FB_CARRITO_KEY = 'fb_carrito';

function obtenerCarrito() {
    try { return JSON.parse(localStorage.getItem(FB_CARRITO_KEY)) || []; }
    catch(e) { return []; }
}

function guardarCarrito(carrito) {
    localStorage.setItem(FB_CARRITO_KEY, JSON.stringify(carrito));
}

// =====================================================
// Modal de cantidad — aparece al hacer clic en "+"
// =====================================================
var _productoModal = null;
var _modalTimeout  = null;

function mostrarModalCantidad(producto) {
    // Si el modal anterior aún esperaba cerrarse, cancelarlo: si no, cerraría el nuevo.
    if (_modalTimeout) { clearTimeout(_modalTimeout); _modalTimeout = null; }
    _productoModal = producto;

    // Crear el modal si no existe
    var modal = document.getElementById('modal-cantidad-carrito');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'modal-cantidad-carrito';
        modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.45);z-index:9000;display:flex;align-items:center;justify-content:center;padding:20px';
        modal.innerHTML = `
          <div style="background:white;border-radius:18px;padding:28px 28px 22px;width:100%;max-width:340px;box-shadow:0 8px 32px rgba(61,28,2,.18)">
            <div id="mc-img-wrap" style="width:100%;height:140px;background:var(--crema-2);border-radius:12px;overflow:hidden;display:flex;align-items:center;justify-content:center;margin-bottom:16px">
              <img id="mc-img" src="" alt="" style="width:100%;height:100%;object-fit:cover;display:none">
              <span id="mc-emoji" style="font-size:52px;display:none"></span>
            </div>
            <div id="mc-categoria" style="font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.1em;color:var(--texto-claro);margin-bottom:4px"></div>
            <div id="mc-nombre" style="font-family:var(--fuente-titulo);font-size:22px;color:var(--chocolate);margin-bottom:4px"></div>
            <div id="mc-precio" style="font-family:var(--fuente-titulo);font-size:26px;color:var(--caramelo);font-weight:700;margin-bottom:18px"></div>
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:18px">
              <span style="font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:var(--texto-claro)">Cantidad</span>
              <div style="display:flex;align-items:center;gap:0;border:1.5px solid var(--borde);border-radius:10px;overflow:hidden">
                <button onclick="cambiarCantidadModal(-1)" style="width:40px;height:40px;background:none;border:none;font-size:22px;cursor:pointer;color:var(--chocolate);transition:background .15s" onmouseover="this.style.background='var(--crema-2)'" onmouseout="this.style.background='none'">−</button>
                <span id="mc-cant" style="min-width:44px;text-align:center;font-family:var(--fuente-titulo);font-size:20px;font-weight:700;color:var(--chocolate)">1</span>
                <button onclick="cambiarCantidadModal(1)"  style="width:40px;height:40px;background:none;border:none;font-size:22px;cursor:pointer;color:var(--chocolate);transition:background .15s" onmouseover="this.style.background='var(--crema-2)'" onmouseout="this.style.background='none'">+</button>
              </div>
            </div>
            <div id="mc-stock-info" style="font-size:12px;color:var(--texto-claro);text-align:right;margin-bottom:14px"></div>
            <button id="mc-btn-agregar" onclick="confirmarAgregarCarrito()" style="width:100%;padding:15px;background:var(--caramelo);color:white;border:none;border-radius:999px;font-family:var(--fuente-cuerpo);font-size:15px;font-weight:700;cursor:pointer;transition:background .2s">
              Agregar al carrito
            </button>
            <button onclick="cerrarModalCantidad()" style="width:100%;padding:10px;background:none;border:none;font-size:13px;color:var(--texto-claro);cursor:pointer;margin-top:6px">
              Cancelar
            </button>
          </div>`;
        document.body.appendChild(modal);
        modal.addEventListener('click', function(e) {
            if (e.target === modal) cerrarModalCantidad();
        });
    }

    // Poblar con datos del producto
    var cant  = 1;
    var stock = producto.stock || 99;
    var img   = document.getElementById('mc-img');
    var emoji = document.getElementById('mc-emoji');

    // Nunca borrar el <img> del modal: solo alternar con el emoji de respaldo.
    if (producto.imagen) {
        img.style.display = 'block';
        if (emoji) emoji.style.display = 'none';
        img.onerror = function () {
            this.style.display = 'none';
            if (emoji) { emoji.textContent = producto.emoji || '🍰'; emoji.style.display = 'block'; }
        };
        img.src = '/img/productos/' + producto.imagen;
    } else {
        img.style.display = 'none';
        img.removeAttribute('src');
        img.onerror = null;
        if (emoji) { emoji.textContent = producto.emoji || '🍰'; emoji.style.display = 'block'; }
    }

    document.getElementById('mc-categoria').textContent  = producto.categoria || '';
    document.getElementById('mc-nombre').textContent     = producto.nombre_producto || producto.nombre || '';
    document.getElementById('mc-cant').textContent       = cant;
    document.getElementById('mc-stock-info').textContent = stock <= 5 ? '⚠ Solo ' + stock + ' disponibles' : stock + ' disponibles';
    actualizarPrecioModal(cant, producto.precio);

    // Reiniciar el botón por si el producto anterior dejó el estado "Agregado".
    var btnAgregar = document.getElementById('mc-btn-agregar');
    if (btnAgregar) {
        btnAgregar.style.background = 'var(--caramelo)';
        btnAgregar.textContent = 'Agregar al carrito';
    }

    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function actualizarPrecioModal(cant, precio) {
    var el = document.getElementById('mc-precio');
    if (el) el.textContent = formatearPrecio(precio * cant);
}

function cambiarCantidadModal(delta) {
    if (!_productoModal) return;
    var el    = document.getElementById('mc-cant');
    var stock = _productoModal.stock || 99;
    var actual = parseInt(el.textContent) || 1;
    var nuevo  = Math.max(1, Math.min(stock, actual + delta));
    el.textContent = nuevo;
    actualizarPrecioModal(nuevo, _productoModal.precio);
}

function confirmarAgregarCarrito() {
    if (!obtenerSesion()) {
        window.location.href = '/login.html';
        return;
    }
    if (!_productoModal) return;
    var cant = parseInt(document.getElementById('mc-cant').textContent) || 1;
    var carrito = obtenerCarrito();
    var nombre  = _productoModal.nombre_producto || _productoModal.nombre || '';
    var pos = carrito.findIndex(function(i) { return i.id_producto === _productoModal.id_producto; });

    if (pos >= 0) {
        carrito[pos].cantidad += cant;
    } else {
        carrito.push({
            id_producto: _productoModal.id_producto,
            nombre:      nombre,
            precio:      parseFloat(_productoModal.precio),
            imagen:      _productoModal.imagen || null,
            emoji:       _productoModal.emoji  || '🍰',
            categoria:   _productoModal.categoria || '',
            cantidad:    cant
        });
    }

    guardarCarrito(carrito);
    actualizarBadgeCarrito();

    // Feedback en el botón
    var btn = document.getElementById('mc-btn-agregar');
    btn.style.background = '#2d7a5f';
    btn.textContent = '✓ Agregado (' + cant + ')';
    _modalTimeout = setTimeout(cerrarModalCantidad, 800);
    mostrarToast(cant + ' × ' + nombre + ' agregado al carrito');
}

function cerrarModalCantidad() {
    if (_modalTimeout) { clearTimeout(_modalTimeout); _modalTimeout = null; }
    var modal = document.getElementById('modal-cantidad-carrito');
    if (modal) modal.style.display = 'none';
    document.body.style.overflow = '';
    _productoModal = null;
}

// agregarAlCarrito ahora abre el modal de cantidad.
// Solo usuarios con sesión iniciada pueden agregar productos al carrito.
function agregarAlCarrito(producto) {
    if (!obtenerSesion()) {
        window.location.href = '/login.html';
        return;
    }
    mostrarModalCantidad(producto);
}

// =====================================================
// Resto de funciones del carrito
// =====================================================
function cambiarCantidad(idProducto, nuevaCantidad) {
    if (nuevaCantidad <= 0) { eliminarDelCarrito(idProducto); return; }
    var carrito = obtenerCarrito();
    var pos = carrito.findIndex(function(i) { return i.id_producto === idProducto; });
    if (pos >= 0) { carrito[pos].cantidad = nuevaCantidad; guardarCarrito(carrito); renderizarCarrito(); }
}

function eliminarDelCarrito(idProducto) {
    var carrito = obtenerCarrito().filter(function(i) { return i.id_producto !== idProducto; });
    guardarCarrito(carrito);
    renderizarCarrito();
    actualizarBadgeCarrito();
}

function vaciarCarrito() {
    guardarCarrito([]);
    renderizarCarrito();
    actualizarBadgeCarrito();
}

function calcularSubtotal() {
    return obtenerCarrito().reduce(function(acc, i) { return acc + i.precio * i.cantidad; }, 0);
}

function actualizarBadgeCarrito() {
    var total = obtenerCarrito().reduce(function(acc, i) { return acc + i.cantidad; }, 0);
    document.querySelectorAll('.carrito-badge').forEach(function(badge) {
        badge.textContent = total;
        badge.style.display = total > 0 ? 'flex' : 'none';
    });
}

function renderizarCarrito() {
    var contenedor = document.getElementById('lista-carrito');
    if (!contenedor) return;
    var carrito = obtenerCarrito();

    if (carrito.length === 0) {
        contenedor.innerHTML = '<p style="padding:32px;text-align:center;color:var(--texto-claro)">Tu carrito está vacío. <a href="catalogo.html" style="color:var(--caramelo)">Ver catálogo</a></p>';
        actualizarResumenCarrito();
        return;
    }

    contenedor.innerHTML = carrito.map(function(item) {
        var imgHTML = item.imagen
            ? '<img src="img/productos/' + item.imagen + '" alt="' + item.nombre + '" style="width:60px;height:60px;object-fit:cover;border-radius:10px" onerror="this.style.display=\'none\'">'
            : '<div style="width:60px;height:60px;background:var(--crema-2);border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:28px">' + (item.emoji || '🍰') + '</div>';

        return '<div style="display:flex;align-items:center;gap:16px;padding:18px 0;border-bottom:1px solid var(--borde)">'
            + imgHTML
            + '<div style="flex:1">'
            + '<div style="font-weight:600;color:var(--chocolate);margin-bottom:4px">' + item.nombre + '</div>'
            + '<div style="font-size:13px;color:var(--texto-claro)">' + item.categoria + '</div>'
            + '</div>'
            + '<div style="display:flex;align-items:center;gap:8px">'
            + '<button onclick="cambiarCantidad(' + item.id_producto + ',' + (item.cantidad - 1) + ')" style="width:28px;height:28px;border-radius:50%;border:1px solid var(--borde);background:white;cursor:pointer;font-size:16px">−</button>'
            + '<span style="font-weight:600;min-width:20px;text-align:center">' + item.cantidad + '</span>'
            + '<button onclick="cambiarCantidad(' + item.id_producto + ',' + (item.cantidad + 1) + ')" style="width:28px;height:28px;border-radius:50%;border:1px solid var(--borde);background:white;cursor:pointer;font-size:16px">+</button>'
            + '</div>'
            + '<div style="min-width:90px;text-align:right">'
            + '<div style="font-family:var(--fuente-titulo);font-size:17px;color:var(--caramelo)">' + formatearPrecio(item.precio * item.cantidad) + '</div>'
            + '<button onclick="eliminarDelCarrito(' + item.id_producto + ')" style="font-size:11px;color:var(--texto-claro);background:none;border:none;cursor:pointer;margin-top:4px">Eliminar</button>'
            + '</div>'
            + '</div>';
    }).join('');

    actualizarResumenCarrito();
}

function actualizarResumenCarrito() {
    var subtotal  = calcularSubtotal();
    var descuento = obtenerDescuento ? obtenerDescuento() : 0;
    var total     = subtotal + 7000 - descuento;
    var elSub  = document.getElementById('resumen-subtotal');
    var elDesc = document.getElementById('resumen-descuento');
    var elTot  = document.getElementById('resumen-total');
    if (elSub)  elSub.textContent  = formatearPrecio(subtotal);
    if (elDesc) elDesc.textContent = descuento > 0 ? '-' + formatearPrecio(descuento) : '—';
    if (elTot)  elTot.textContent  = formatearPrecio(Math.max(total, 0));
}
