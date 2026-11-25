package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pruebas del contrato HTTP real (Tomcat en un puerto libre, perfil {@code memoria}).
 *
 * <p>Comprueba lo que ve la app movil: la API responde 401 sin sesion, las paginas redirigen al
 * login y la respuesta entrega la cookie {@code XSRF-TOKEN} que el cliente devuelve como cabecera
 * {@code X-XSRF-TOKEN} en sus POST/PUT/DELETE.</p>
 *
 * <p>Esta comprobacion no se puede hacer con MockMvc: en cuanto una clase de pruebas usa
 * {@code csrf()}, spring-security-test cambia el {@code CookieCsrfTokenRepository} de la cadena
 * por un repositorio de sesion y la respuesta deja de traer la cookie. Con HTTP real se valida el
 * comportamiento del servidor tal cual.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("memoria")
class CookieCsrfHttpTests {

    @Value("${local.server.port}")
    private int puerto;

    private URI url(String ruta) {
        return URI.create("http://localhost:" + this.puerto + ruta);
    }

    /** Cliente que guarda cookies y las reenvia, como el navegador o la app. */
    private HttpClient clienteConCookies() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    @Test
    void sesionSinLoginDevuelve401YEntregaLaCookieCsrf() throws Exception {
        HttpResponse<String> respuesta = clienteConCookies().send(
                HttpRequest.newBuilder(url("/api/sesion")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(401, respuesta.statusCode());
        List<String> cookies = respuesta.headers().allValues("Set-Cookie");
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN=")),
                "sin cookie XSRF-TOKEN; Set-Cookie recibido: " + cookies);
    }

    @Test
    void apiPrivadaSinSesionDevuelve401() throws Exception {
        HttpResponse<String> respuesta = clienteConCookies().send(
                HttpRequest.newBuilder(url("/api/pagos")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(401, respuesta.statusCode());
    }

    @Test
    void panelSinSesionRedirigeAlLogin() throws Exception {
        HttpResponse<String> respuesta = clienteConCookies().send(
                HttpRequest.newBuilder(url("/admin/dashboard")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(302, respuesta.statusCode());
        String destino = respuesta.headers().firstValue("Location").orElse("");
        assertTrue(destino.endsWith("/login.html"), "destino inesperado: " + destino);
    }
}
