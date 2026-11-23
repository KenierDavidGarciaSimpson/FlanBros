// sesion.js — Manejo de autenticación y roles

const FB_USUARIO_KEY = 'fb_usuario';

function obtenerSesion() {
    try { return JSON.parse(localStorage.getItem(FB_USUARIO_KEY)) || null; }
    catch(e) { return null; }
}

function guardarSesion(usuario) {
    localStorage.setItem(FB_USUARIO_KEY, JSON.stringify(usuario));
}

function cerrarSesion() {
    localStorage.removeItem(FB_USUARIO_KEY);
    // Cierra tambien la sesion del servidor (invalida la cookie JSESSIONID).
    var ir = function () { window.location.href = '/login.html'; };
    fetch('/api/logout', { method: 'POST' }).then(ir, ir);
}

// Redirige al login si no hay sesión activa
function requiereSesion() {
    if (!obtenerSesion()) {
        window.location.href = '/login.html';
    }
}

// Redirige si el usuario no tiene un rol admin
function requiereAdmin() {
    const u = obtenerSesion();
    const rolesAdmin = ['administrador', 'repostero', 'contador', 'domiciliario'];
    if (!u || !rolesAdmin.includes(u.nombre_rol)) {
        window.location.href = '/login.html';
    }
}
