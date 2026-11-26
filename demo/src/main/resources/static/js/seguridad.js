(function () {
    'use strict';
    var fetchOriginal = window.fetch.bind(window);

    function leerCookie(nombre) {
        var partes = ('; ' + document.cookie).split('; ' + nombre + '=');
        return partes.length === 2 ? decodeURIComponent(partes.pop().split(';').shift()) : null;
    }

    function esMismoSitio(url) {
        return url.charAt(0) === '/' && url.charAt(1) !== '/' || url.indexOf(window.location.origin) === 0;
    }

    window.fetch = function (entrada, opciones) {
        opciones = opciones || {};
        var url = typeof entrada === 'string' ? entrada : ((entrada && entrada.url) || '');
        var metodo = (opciones.method || (entrada && entrada.method) || 'GET').toUpperCase();
        var mismoSitio = esMismoSitio(url);

        if (mismoSitio && ['GET', 'HEAD', 'OPTIONS'].indexOf(metodo) === -1) {
            var token = leerCookie('XSRF-TOKEN');
            if (token) {
                var cabeceras = new Headers(opciones.headers || (entrada && entrada.headers) || undefined);
                cabeceras.set('X-XSRF-TOKEN', token);
                opciones.headers = cabeceras;
            }
        }

        return fetchOriginal(entrada, opciones).then(function (respuesta) {
            var esAutenticacion = /\/api\/(login|registro|recuperar|logout)/.test(url);
            if (respuesta.status === 401 && mismoSitio && !esAutenticacion) {
                try { localStorage.removeItem('fb_usuario'); } catch (e) {  }
                if (window.location.pathname.indexOf('/login') !== 0) {
                    window.location.href = '/login.html';
                }
            }
            return respuesta;
        });
    };
})();
