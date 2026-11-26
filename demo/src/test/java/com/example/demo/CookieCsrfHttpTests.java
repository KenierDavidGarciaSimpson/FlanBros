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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("memoria")
class CookieCsrfHttpTests {

    @Value("${local.server.port}")
    private int puerto;

    private URI url(String ruta) {
        return URI.create("http://localhost:" + this.puerto + ruta);
    }

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
