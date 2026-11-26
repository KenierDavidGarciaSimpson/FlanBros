var _u = obtenerSesion();
var _rolesAdmin = ['administrador', 'repostero', 'contador', 'domiciliario'];

if (!_u || !_rolesAdmin.includes(_u.nombre_rol)) {
    if (window.location.pathname.indexOf('dashboard') !== -1) {
        window.location.replace('../login.html');
    }
} else if (_u.nombre_rol === 'domiciliario') {
    window.location.replace('domiciliario.html');
}

var _elAdmin = document.getElementById('nombre-admin-topbar');
if (_elAdmin && _u) _elAdmin.textContent = _u.nombre + ' · ' + _u.nombre_rol;

var _permisos = {
    administrador: ['dashboard','pedidos','productos','inventario','clientes','pagos','facturas','domicilios','empleados','mensajes'],
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
    var secciones = ['dashboard','pedidos','productos','inventario','clientes','pagos','facturas','domicilios','empleados','mensajes'];
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
        facturas:'Facturas', domicilios:'Domicilios', empleados:'Empleados',
        mensajes:'Mensajes'
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
    if (nombre === 'mensajes')   cargarMensajesAdmin();
}

function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
}

mostrarSeccion(_permitidas[0] || 'dashboard');
