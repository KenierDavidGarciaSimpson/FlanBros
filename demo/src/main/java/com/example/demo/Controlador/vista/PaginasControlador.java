package com.example.demo.Controlador.vista;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * PaginasControlador — lado servidor de la navegacion de las vistas.
 *
 * <p>CONVERTIDO desde: la navegacion que hacian los JS del frontend
 * ({@code window.location.href} en sesion.js, admin-nav.js y domiciliario.js) y los
 * enlaces {@code href="catalogo.html"} de las plantillas.</p>
 *
 * <p>Cada metodo devuelve el nombre de la plantilla dentro de
 * {@code src/main/resources/templates}; no hay logica ni acceso a datos aqui.</p>
 *
 * <p>Los alias con extension ({@code /catalogo.html}) estan a proposito: permiten que
 * los enlaces actuales de los HTML resuelvan contra el servidor sin modificar ni una
 * linea de las plantillas.</p>
 *
 * <p>Capa: Controlador. La base de datos solo se consulta desde Persistencias.</p>
 */
@Controller
public class PaginasControlador {

    // ------------------------------------------------------------------
    // Sitio publico
    // ------------------------------------------------------------------

    @GetMapping({ "/", "/index", "/index.html" })
    public String index() {
        return "index";
    }

    @GetMapping({ "/catalogo", "/catalogo.html" })
    public String catalogo() {
        return "catalogo";
    }

    /**
     * producto.html vive en templates/pages porque sus rutas internas son relativas
     * al estilo "../css/estilos.css", "../js/modules/..." y "../catalogo.html".
     * Servido en /pages/producto.html todas resuelven; en /producto.html romperian.
     * catalogo.js enlaza justamente a pages/producto.html?id=N.
     */
    @GetMapping({ "/pages/producto", "/pages/producto.html", "/producto", "/producto.html" })
    public String producto() {
        return "pages/producto";
    }

    @GetMapping({ "/carrito", "/carrito.html" })
    public String carrito() {
        return "carrito";
    }

    @GetMapping({ "/checkout", "/checkout.html" })
    public String checkout() {
        return "checkout";
    }

    @GetMapping({ "/contacto", "/contacto.html" })
    public String contacto() {
        return "contacto";
    }

    @GetMapping({ "/nosotros", "/nosotros.html" })
    public String nosotros() {
        return "nosotros";
    }

    @GetMapping({ "/cuenta", "/cuenta.html" })
    public String cuenta() {
        return "cuenta";
    }

    // ------------------------------------------------------------------
    // Autenticacion (solo vistas; la validacion real es otra tarea)
    // ------------------------------------------------------------------

    @GetMapping({ "/login", "/login.html" })
    public String login() {
        return "login";
    }

    @GetMapping({ "/registro", "/registro.html" })
    public String registro() {
        return "registro";
    }

    @GetMapping({ "/recuperar", "/recuperar.html" })
    public String recuperar() {
        return "recuperar";
    }

    // ------------------------------------------------------------------
    // Panel interno (plantillas dentro de templates/admin)
    // ------------------------------------------------------------------

    @GetMapping({ "/admin", "/admin/dashboard", "/admin/dashboard.html" })
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping({ "/admin/domiciliario", "/admin/domiciliario.html" })
    public String adminDomiciliario() {
        return "admin/domiciliario";
    }
}
