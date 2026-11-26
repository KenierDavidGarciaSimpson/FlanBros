var categoriaActiva = 'Todos';
var paginaActual    = 1;
var ITEMS_POR_PAG   = 8;

function cargarCatalogo() {
    var grilla = document.getElementById('grilla-productos');
    if (!grilla) return;

    grilla.innerHTML = '<p style="color:var(--texto-claro);padding:40px;text-align:center">Cargando productos...</p>';

    obtenerProductos(true)
        .then(function() { filtrarProductos(); })
        .catch(function() {
            grilla.innerHTML = '<p style="color:var(--texto-claro);padding:40px">No se pudieron cargar los productos. Revisa tu conexión y recarga la página.</p>';
        });
}

function filtrarProductos() {
    var grilla   = document.getElementById('grilla-productos');
    if (!grilla) return;

    paginaActual = 1;

    var busqueda = (document.getElementById('buscador')?.value || '').toLowerCase();
    var orden    = document.getElementById('ordenar')?.value || 'popular';
    var prods    = window.PRODUCTOS || [];

    var lista = prods.filter(function(p) {
        var matchCat  = categoriaActiva === 'Todos' || p.categoria === categoriaActiva;
        var matchBusc = (p.nombre_producto || '').toLowerCase().includes(busqueda)
                     || (p.descripcion || '').toLowerCase().includes(busqueda);
        return matchCat && matchBusc;
    });

    if (orden === 'precio-asc')  lista.sort(function(a,b) { return a.precio - b.precio; });
    if (orden === 'precio-desc') lista.sort(function(a,b) { return b.precio - a.precio; });
    if (orden === 'nombre')      lista.sort(function(a,b) { return a.nombre_producto.localeCompare(b.nombre_producto); });

    window._LISTA_FILTRADA = lista;
    renderizarPagina();
}

function renderizarPagina() {
    var grilla = document.getElementById('grilla-productos');
    if (!grilla) return;

    var lista      = window._LISTA_FILTRADA || [];
    var totalPags  = Math.ceil(lista.length / ITEMS_POR_PAG) || 1;
    paginaActual   = Math.min(paginaActual, totalPags);

    var inicio = (paginaActual - 1) * ITEMS_POR_PAG;
    var fin    = inicio + ITEMS_POR_PAG;
    var pagina = lista.slice(inicio, fin);

    if (pagina.length === 0) {
        grilla.innerHTML = '<p style="color:var(--texto-claro);padding:40px;grid-column:1/-1">No se encontraron productos.</p>';
        renderizarPaginacion(totalPags);
        return;
    }

    grilla.innerHTML = pagina.map(function(p) {
        var agotado = !estaDisponible(p);
        var nombre  = escaparHTML(p.nombre_producto);
        var rutaImg = p.imagen ? 'img/productos/' + escaparHTML(p.imagen) : null;
        var emoji   = escaparHTML(p.emoji || '🍰');

        var imgHTML = rutaImg
            ? '<img src="' + rutaImg + '" alt="' + nombre + '" style="width:100%;height:100%;object-fit:cover" onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'flex\'">'
              + '<div style="display:none;width:100%;height:100%;align-items:center;justify-content:center;font-size:52px">' + emoji + '</div>'
            : '<div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;font-size:52px">' + emoji + '</div>';

        return '<div class="tarjeta-producto">'
            + '<a href="pages/producto.html?id=' + p.id_producto + '">'
            + '<div class="tarjeta-imagen" style="overflow:hidden;padding:0">'
            + imgHTML
            + (p.badge ? '<span class="tarjeta-badge' + (p.badge === 'Nuevo' ? ' nuevo' : '') + '">' + escaparHTML(p.badge) + '</span>' : '')
            + (agotado ? '<span class="tarjeta-badge" style="background:#888">Agotado</span>' : '')
            + '</div>'
            + '</a>'
            + '<div class="tarjeta-cuerpo">'
            + '<div class="tarjeta-categoria">' + escaparHTML(p.categoria) + '</div>'
            + '<a href="pages/producto.html?id=' + p.id_producto + '" class="tarjeta-nombre">' + nombre + '</a>'
            + '<p class="tarjeta-descripcion">' + escaparHTML(p.descripcion) + '</p>'
            + '<div class="tarjeta-pie">'
            + '<span class="tarjeta-precio">' + formatearPrecio(p.precio) + '</span>'
            + (agotado
                ? '<span style="font-size:12px;color:#888">Sin stock</span>'
                : '<button type="button" class="btn-agregar" aria-label="Agregar ' + nombre + ' al carrito" onclick="agregarAlCarrito(' + p.id_producto + ')">+</button>')
            + '</div>'
            + '</div>'
            + '</div>';
    }).join('');

    renderizarPaginacion(totalPags);
}

function renderizarPaginacion(totalPags) {
    var cont = document.getElementById('paginacion-catalogo');
    if (!cont) return;

    if (totalPags <= 1) { cont.innerHTML = ''; return; }

    var html = '<div style="display:flex;gap:8px;justify-content:center;margin-top:40px;flex-wrap:wrap">';

    html += '<button onclick="cambiarPagina(' + (paginaActual - 1) + ')" '
          + (paginaActual === 1 ? 'disabled' : '')
          + ' style="padding:8px 16px;border:1.5px solid var(--borde);border-radius:8px;background:white;cursor:pointer;color:var(--chocolate)">← Anterior</button>';

    for (var i = 1; i <= totalPags; i++) {
        var activo = i === paginaActual;
        html += '<button onclick="cambiarPagina(' + i + ')" style="'
              + 'padding:8px 14px;border-radius:8px;cursor:pointer;font-weight:600;'
              + (activo
                  ? 'background:var(--caramelo);color:white;border:1.5px solid var(--caramelo)'
                  : 'background:white;color:var(--chocolate);border:1.5px solid var(--borde)')
              + '">' + i + '</button>';
    }

    html += '<button onclick="cambiarPagina(' + (paginaActual + 1) + ')" '
          + (paginaActual === totalPags ? 'disabled' : '')
          + ' style="padding:8px 16px;border:1.5px solid var(--borde);border-radius:8px;background:white;cursor:pointer;color:var(--chocolate)">Siguiente →</button>';

    html += '</div>';
    cont.innerHTML = html;
}

function cambiarPagina(num) {
    var totalPags = Math.ceil((window._LISTA_FILTRADA || []).length / ITEMS_POR_PAG) || 1;
    if (num < 1 || num > totalPags) return;
    paginaActual = num;
    renderizarPagina();
    var grilla = document.getElementById('grilla-productos');
    if (grilla) grilla.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function seleccionarCategoria(cat) {
    categoriaActiva = cat;
    document.querySelectorAll('.categoria-pill').forEach(function(pill) {
        pill.classList.toggle('activa', pill.dataset.categoria === cat);
    });
    filtrarProductos();
}
