package com.example.demo;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.Modelo.dto.SesionUsuario;

@SpringBootTest
@ActiveProfiles("memoria")
class SeguridadApiTests {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private static RequestPostProcessor como(String rol, int idEntidad) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.id_usuario = 900 + idEntidad;
        sesion.id_entidad = idEntidad;
        sesion.nombre = "Prueba";
        sesion.nombre_rol = rol;
        return authentication(UsernamePasswordAuthenticationToken.authenticated(sesion, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase(Locale.ROOT)))));
    }

    @Test
    void catalogoEsPublico() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
    }

    @Test
    void apiPrivadaSinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/pagos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void panelSinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login.html")));
        mvc.perform(get("/cuenta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login.html")));
    }

    @Test
    void sesionSinLoginDevuelve401YExponeElTokenCsrf() throws Exception {
        mvc.perform(get("/api/sesion"))
                .andExpect(status().isUnauthorized())
                .andExpect(request().attribute("_csrf", notNullValue()))
                .andExpect(request().attribute("_csrf",
                        hasProperty("token", not(emptyOrNullString()))));
    }

    @Test
    void sesionConLoginDevuelveElUsuario() throws Exception {
        mvc.perform(get("/api/sesion").with(como("cliente", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.usuario.nombre_rol").value("cliente"));
    }

    @Test
    void clienteNoPuedeVerPagos() throws Exception {
        mvc.perform(get("/api/pagos").with(como("cliente", 1))).andExpect(status().isForbidden());
    }

    @Test
    void contadorPuedeVerPagosPeroNoEmpleados() throws Exception {
        mvc.perform(get("/api/pagos").with(como("contador", 5))).andExpect(status().isOk());
        mvc.perform(get("/api/empleados").with(como("contador", 5))).andExpect(status().isForbidden());
    }

    @Test
    void soloAdministradorVeEmpleadosYClientes() throws Exception {
        mvc.perform(get("/api/empleados").with(como("repostero", 2))).andExpect(status().isForbidden());
        mvc.perform(get("/api/empleados").with(como("administrador", 1))).andExpect(status().isOk());
        mvc.perform(get("/api/clientes").with(como("administrador", 1))).andExpect(status().isOk());
    }

    @Test
    void reposteroGestionaInventarioPeroNoDomicilios() throws Exception {
        mvc.perform(get("/api/inventario").with(como("repostero", 2))).andExpect(status().isOk());
        mvc.perform(get("/api/domicilios").with(como("repostero", 2))).andExpect(status().isForbidden());
    }

    @Test
    void escrituraSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/api/pedidos").with(como("cliente", 1))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void escrituraConTokenCsrfLlegaAlControlador() throws Exception {
        mvc.perform(post("/api/pedidos").with(como("cliente", 1)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"laura@demo.com\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginCorrectoDevuelveElUsuario() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"laura@demo.com\",\"contrasena\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.usuario.nombre_rol").value("cliente"));
    }

    @Test
    void tras5FallosElLoginSeBloquea() throws Exception {
        String cuerpo = "{\"correo\":\"intruso@demo.com\",\"contrasena\":\"x\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void clienteSoloVeSusPropiosPedidos() throws Exception {
        mvc.perform(get("/api/pedidos").param("id_cliente", "2").with(como("cliente", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[*].id_cliente", everyItem(equalTo(1))));
    }

    @Test
    void domiciliarioNoPuedeCancelarPedidos() throws Exception {
        mvc.perform(put("/api/pedidos").with(como("domiciliario", 3)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_pedido\":1,\"estado\":\"cancelado\"}"))
                .andExpect(status().isForbidden());
    }
}
