package com.example.demo.Seguridad;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SolicitudCsrfSpa — manejo del token CSRF para un frontend de JavaScript.
 *
 * <p>El token viaja en la cookie {@code XSRF-TOKEN} (legible por JS) y {@code seguridad.js} lo
 * devuelve en la cabecera {@code X-XSRF-TOKEN} en cada POST/PUT/DELETE. Una pagina de otro sitio
 * no puede leer esa cookie, por lo que no puede fabricar peticiones validas.</p>
 *
 * <p>Es el manejador que recomienda la documentacion de Spring Security para aplicaciones SPA.</p>
 *
 * <p>Capa: Seguridad.</p>
 */
final class SolicitudCsrfSpa implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plano = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        this.xor.handle(request, response, csrfToken);
        // Forzar la carga del token para que la cookie se escriba tambien en las peticiones GET.
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String cabecera = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(cabecera) ? this.plano : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
