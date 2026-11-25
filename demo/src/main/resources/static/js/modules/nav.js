// nav.js — Estado del nav según sesión activa

function actualizarNav() {
    const usuario  = obtenerSesion();
    const btnLogin = document.getElementById('btn-nav-login');
    const btnSalir = document.getElementById('btn-nav-salir');
    const nomNav   = document.getElementById('nav-nombre-usuario');

    if (usuario) {
        if (btnLogin) btnLogin.style.display = 'none';
        if (btnSalir) btnSalir.style.display = 'inline-flex';
        if (nomNav)   nomNav.textContent = usuario.nombre;
    } else {
        if (btnLogin) btnLogin.style.display = 'inline-flex';
        if (btnSalir) btnSalir.style.display = 'none';
        if (nomNav)   nomNav.textContent = '';
    }
}

// "Ordenar" solo entra al catálogo con sesión activa; si no, manda al login.
function irAOrdenar(evento) {
    if (evento) evento.preventDefault();
    window.location.href = obtenerSesion() ? '/catalogo.html' : '/login.html';
}
