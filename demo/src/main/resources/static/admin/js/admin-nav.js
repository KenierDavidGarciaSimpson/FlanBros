// admin-nav.js — Acceso, permisos y navegación entre secciones

var _u = obtenerSesion();
var _rolesAdmin = ['administrador', 'repostero', 'contador', 'domiciliario'];

if (!_u || !_rolesAdmin.includes(_u.nombre_rol)) {
    if (window.location.pathname.indexOf('dashboard') !== -1) {
        window.location.replace('../login.html');
    }
} else if (_u.nombre_rol === 'domiciliario') {
    window.location.replace('domiciliario.html');
}

// Nombre en la topbar (ahora dinámico desde la sesión)
var _elAdmin = document.getElementById('nombre-admin-topbar');
if (_elAdmin && _u) _elAdmin.textContent = _u.nombre + ' · ' + _u.nombre_rol;

// Secciones visibles según rol
var _permisos = {
    administrador: ['dashboard','pedidos','productos','inventario','clientes','pagos','facturas','domicilios','empleados'],
    repostero:     ['productos','inventario'],
    contador:      ['facturas','pagos']
};
var _permitidas = _permisos[_u ? _u.nombre_rol : ''] || [];

document.querySelectorAll('.admin-nav-item').forEach(function(btn) {
    var match = (btn.getAttribute('onclick') || '').match(/'([^']+)'/);
    var sec = match ? match[1] : null;
    if (sec && !_permitidas.includes(sec)) btn.style.display = 'none';
});

function mostrarSeccion(nombre) {
    var secciones = ['dashboard','pedidos','productos','inventario','clientes','pagos','facturas','domicilios','empleados'];
    secciones.forEach(function(sec) {
        var el = document.getElementById('sec-' + sec);
        if (el) el.style.display = (sec === nombre) ? 'block' : 'none';
    });
    document.querySelectorAll('.admin-nav-item').forEach(function(btn) {
        var match = (btn.getAttribute('onclick') || '').match(/'([^']+)'/);
        var sec = match ? match[1] : null;
        btn.classList.toggle('activo', sec === nombre);
    });
    var titulos = {
        dashboard:'Dashboard', pedidos:'Pedidos', productos:'Productos',
        inventario:'Inventario', clientes:'Clientes', pagos:'Pagos',
        facturas:'Facturas', domicilios:'Domicilios', empleados:'Empleados'
    };
    var elTit = document.getElementById('titulo-seccion');
    if (elTit) elTit.textContent = titulos[nombre] || nombre;

    if (nombre === 'dashboard')  cargarDashboard();
    if (nombre === 'pedidos')    cargarPedidosAdmin();
    if (nombre === 'productos')  cargarProductosAdmin();
    if (nombre === 'inventario') cargarInventarioAdmin();
    if (nombre === 'clientes')   cargarClientesAdmin();
    if (nombre === 'empleados')  cargarEmpleadosAdmin();
    if (nombre === 'facturas')   cargarFacturasAdmin();
    if (nombre === 'pagos')      cargarPagosAdmin();
    if (nombre === 'domicilios') cargarDomiciliosAdmin();
}

function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
}

mostrarSeccion(_permitidas[0] || 'dashboard');
