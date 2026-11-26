const API = '/api';

function formatearPrecio(num) {
    return '$' + Math.round(num).toLocaleString('es-CO');
}

function formatearFecha(fechaStr) {
    if (!fechaStr) return '—';
    const f = new Date(fechaStr);
    return f.toLocaleDateString('es-CO', { day:'numeric', month:'short', year:'numeric' });
}

function etiquetaEstado(estado) {
    const map = {
        'pendiente':      'Pendiente',
        'en_preparacion': 'En preparación',
        'listo':          'Listo para enviar',
        'entregado':      'Entregado',
        'cancelado':      'Cancelado'
    };
    return map[estado] || estado;
}

function slugificar(texto) {
    return texto.toLowerCase()
        .replace(/á/g,'a').replace(/é/g,'e').replace(/í/g,'i').replace(/ó/g,'o').replace(/ú/g,'u').replace(/ñ/g,'n')
        .replace(/[^a-z0-9\s-]/g,'')
        .trim().replace(/\s+/g,'-');
}

function mostrarToast(mensaje, tipo) {
    const toast = document.createElement('div');
    const color = tipo === 'error' ? '#c0392b' : '#2d7a5f';
    toast.textContent = mensaje;
    toast.style.cssText = 'position:fixed;bottom:24px;right:24px;background:' + color + ';color:white;padding:12px 20px;border-radius:10px;font-size:14px;font-weight:500;z-index:9999;box-shadow:0 4px 16px rgba(0,0,0,0.2);transition:opacity 0.4s';
    document.body.appendChild(toast);
    setTimeout(function() { toast.style.opacity = '0'; setTimeout(function() { toast.remove(); }, 400); }, 2800);
}
