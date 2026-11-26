// ver-contrasena.js — agrega a cada campo de contraseña un botón para mostrarla u ocultarla
// mientras se escribe. Se carga en login, registro, recuperar y el panel de administración.
(function () {
    function agregarBoton(input) {
        if (input.dataset.verContrasena) return;
        input.dataset.verContrasena = '1';

        var envoltura = document.createElement('div');
        envoltura.style.position = 'relative';
        input.parentNode.insertBefore(envoltura, input);
        envoltura.appendChild(input);
        input.style.width = '100%';
        input.style.paddingRight = '76px';
        input.style.boxSizing = 'border-box';

        var boton = document.createElement('button');
        boton.type = 'button';
        boton.textContent = 'Mostrar';
        boton.setAttribute('aria-label', 'Mostrar contraseña');
        boton.style.cssText = 'position:absolute;right:8px;top:50%;transform:translateY(-50%);'
            + 'background:none;border:none;cursor:pointer;font-size:13px;font-weight:600;'
            + 'color:var(--chocolate, #6b3e26);padding:4px 6px';
        boton.addEventListener('click', function () {
            var oculto = input.type === 'password';
            input.type = oculto ? 'text' : 'password';
            boton.textContent = oculto ? 'Ocultar' : 'Mostrar';
            boton.setAttribute('aria-label', oculto ? 'Ocultar contraseña' : 'Mostrar contraseña');
            input.focus();
        });
        envoltura.appendChild(boton);
    }

    function iniciar() {
        document.querySelectorAll('input[type="password"]').forEach(agregarBoton);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();
