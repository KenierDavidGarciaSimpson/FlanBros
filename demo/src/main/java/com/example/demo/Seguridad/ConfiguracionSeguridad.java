package com.example.demo.Seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

/**
 * ConfiguracionSeguridad — quien puede llamar a que, decidido en el SERVIDOR.
 *
 * <p>Antes la sesion vivia solo en {@code localStorage} y cualquiera podia llamar a
 * {@code /api/**} sin iniciar sesion. Ahora:</p>
 * <ul>
 *   <li>El login crea una sesion HTTP (cookie {@code JSESSIONID}, HttpOnly, SameSite=Lax).</li>
 *   <li>Cada ruta de la API exige un rol (matriz de abajo); lo no listado se deniega.</li>
 *   <li>Las peticiones que modifican datos exigen el token CSRF.</li>
 *   <li>Las paginas {@code /admin/**} y {@code /cuenta} redirigen al login si no hay sesion.</li>
 * </ul>
 *
 * <p>La propiedad de los datos (un cliente solo ve SUS pedidos) la aplican los controladores
 * con {@link UsuarioActual}.</p>
 *
 * <p>Capa: Seguridad.</p>
 */
@Configuration
public class ConfiguracionSeguridad {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String REPOSTERO = "REPOSTERO";
    private static final String CONTADOR = "CONTADOR";
    private static final String DOMICILIARIO = "DOMICILIARIO";
    private static final String CLIENTE = "CLIENTE";

    private static final String[] RECURSOS_ESTATICOS = {
            "/css/**", "/js/**", "/img/**", "/admin/js/**", "/favicon.ico", "/error" };

    private static final String[] PAGINAS_PUBLICAS = {
            "/", "/index", "/index.html", "/catalogo", "/catalogo.html",
            "/pages/producto", "/pages/producto.html", "/producto", "/producto.html",
            "/carrito", "/carrito.html", "/checkout", "/checkout.html",
            "/contacto", "/contacto.html", "/nosotros", "/nosotros.html",
            "/login", "/login.html", "/registro", "/registro.html",
            "/recuperar", "/recuperar.html" };

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        RequestMatcher esApi = solicitud -> solicitud.getRequestURI().startsWith("/api/");

        // Sin sesion: la API responde 401 (lo distingue la app movil y el JS) y las paginas
        // redirigen al login. Se elige a mano porque con esta version de Spring Security
        // defaultAuthenticationEntryPointFor(...) no llega a aplicarse y todo acababa en 302.
        LoginUrlAuthenticationEntryPoint entradaPaginas = new LoginUrlAuthenticationEntryPoint("/login.html");
        HttpStatusEntryPoint entradaApi = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SolicitudCsrfSpa()))
            .authorizeHttpRequests(rutas -> rutas
                // ---- Publico ----
                .requestMatchers(RECURSOS_ESTATICOS).permitAll()
                .requestMatchers(PAGINAS_PUBLICAS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/login", "/api/registro", "/api/recuperar",
                        "/api/cupones").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/sesion").authenticated()

                // ---- API por rol ----
                .requestMatchers("/api/productos", "/api/productos/**",
                        "/api/inventario", "/api/inventario/**").hasAnyRole(ADMIN, REPOSTERO)
                .requestMatchers("/api/pagos", "/api/pagos/**").hasAnyRole(ADMIN, CONTADOR)
                .requestMatchers("/api/facturas", "/api/facturas/**").hasAnyRole(ADMIN, CONTADOR, CLIENTE)
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole(CLIENTE)
                .requestMatchers(HttpMethod.GET, "/api/pedidos").hasAnyRole(ADMIN, DOMICILIARIO, CLIENTE)
                .requestMatchers(HttpMethod.PUT, "/api/pedidos").hasAnyRole(ADMIN, DOMICILIARIO)
                .requestMatchers("/api/domicilios", "/api/domicilios/**").hasAnyRole(ADMIN, DOMICILIARIO)
                .requestMatchers(HttpMethod.POST, "/api/contacto").authenticated()
                .requestMatchers("/api/clientes", "/api/clientes/**",
                        "/api/empleados", "/api/empleados/**",
                        "/api/dashboard", "/api/dashboard/**",
                        "/api/contacto", "/api/contacto/**").hasRole(ADMIN)
                .requestMatchers("/api/**").denyAll()

                // ---- Paginas con sesion ----
                .requestMatchers("/cuenta", "/cuenta.html").authenticated()
                .requestMatchers("/admin/**").hasAnyRole(ADMIN, REPOSTERO, CONTADOR, DOMICILIARIO)
                .anyRequest().authenticated())
            .exceptionHandling(errores -> errores
                // Sin sesion: la API responde 401 y las paginas redirigen al login.
                .authenticationEntryPoint((solicitud, respuesta, excepcion) ->
                        (esApi.matches(solicitud) ? entradaApi : entradaPaginas)
                                .commence(solicitud, respuesta, excepcion))
                // Con sesion pero sin permiso (o token CSRF invalido): 403.
                .accessDeniedHandler((solicitud, respuesta, excepcion) -> {
                    if (esApi.matches(solicitud)) {
                        String mensaje = excepcion instanceof CsrfException
                                ? "La sesion de seguridad vencio. Recarga la pagina e intenta de nuevo"
                                : "No tienes permiso para esta accion";
                        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        respuesta.setContentType("application/json;charset=UTF-8");
                        respuesta.getWriter().write(
                                "{\"exito\":false,\"mensaje\":\"" + mensaje + "\",\"datos\":[]}");
                    } else {
                        respuesta.sendRedirect("/");
                    }
                }))
            .logout(salida -> salida
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((solicitud, respuesta, autenticacion) ->
                        respuesta.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .deleteCookies("JSESSIONID"));

        return http.build();
    }
}
