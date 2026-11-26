var VALIDA = {
    correo: function (v) {
        v = (v || '').trim();
        if (!v) return 'El correo es obligatorio.';
        if (v.length > 120 || !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v)) {
            return 'Escribe un correo válido (ejemplo: nombre@correo.com).';
        }
        return null;
    },
    nombre: function (campo, v) {
        v = (v || '').trim();
        if (!v) return 'El campo ' + campo + ' es obligatorio.';
        if (v.length < 2 || v.length > 80) return 'El campo ' + campo + ' debe tener entre 2 y 80 caracteres.';
        if (!/^\p{L}[\p{L} .'-]*$/u.test(v)) return 'El campo ' + campo + ' solo puede tener letras y espacios.';
        return null;
    },
    telefono: function (v, obligatorio) {
        v = (v || '').replace(/[\s-]/g, '');
        if (!v) return obligatorio ? 'El teléfono es obligatorio.' : null;
        if (!/^\+?[0-9]{7,15}$/.test(v)) return 'El teléfono debe tener entre 7 y 15 dígitos.';
        return null;
    },
    contrasena: function (v) {
        v = v || '';
        if (v.length < 8) return 'La contraseña debe tener al menos 8 caracteres.';
        if (v.length > 72) return 'La contraseña no puede tener más de 72 caracteres.';
        return null;
    },
    largo: function (campo, v, minimo, maximo) {
        v = (v || '').trim();
        if (minimo && v.length < minimo) return 'El campo ' + campo + ' debe tener al menos ' + minimo + ' caracteres.';
        if (maximo && v.length > maximo) return 'El campo ' + campo + ' no puede tener más de ' + maximo + ' caracteres.';
        return null;
    },
    primero: function () {
        for (var i = 0; i < arguments.length; i++) { if (arguments[i]) return arguments[i]; }
        return null;
    }
};

function mostrarError(div, mensaje) {
    if (div) { div.textContent = mensaje; div.style.display = 'block'; }
    else { mostrarToast(mensaje, 'error'); }
}
