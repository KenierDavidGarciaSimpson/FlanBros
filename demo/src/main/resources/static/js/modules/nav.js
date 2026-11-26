function actualizarNav() {
    const usuario  = obtenerSesion();
    const btnLogin = document.getElementById('btn-nav-login');
    const btnSalir = document.getElementById('btn-nav-salir');
    const nomNav   = document.getElementById('nav-nombre-usuario');
    const btnPanel = document.getElementById('btn-nav-panel');
    const rolesPanel = ['administrador', 'repostero', 'contador', 'domiciliario'];

    if (usuario) {
        if (btnLogin) btnLogin.style.display = 'none';
        if (btnSalir) btnSalir.style.display = 'inline-flex';
        if (nomNav)   nomNav.textContent = usuario.nombre;
    } else {
        if (btnLogin) btnLogin.style.display = 'inline-flex';
        if (btnSalir) btnSalir.style.display = 'none';
        if (nomNav)   nomNav.textContent = '';
    }

    if (btnPanel) {
        if (usuario && rolesPanel.includes(usuario.nombre_rol)) {
            btnPanel.href = usuario.nombre_rol === 'domiciliario'
                ? '/admin/domiciliario.html'
                : '/admin/dashboard.html';
            btnPanel.style.display = 'inline-flex';
        } else {
            btnPanel.style.display = 'none';
        }
    }
}

function irAOrdenar(evento) {
    if (evento) evento.preventDefault();
    window.location.href = obtenerSesion() ? '/catalogo.html' : '/login.html';
}
