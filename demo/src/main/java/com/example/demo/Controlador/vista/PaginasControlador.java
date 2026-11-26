package com.example.demo.Controlador.vista;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginasControlador {

    @GetMapping({ "/", "/index", "/index.html" })
    public String index() {
        return "index";
    }

    @GetMapping({ "/catalogo", "/catalogo.html" })
    public String catalogo() {
        return "catalogo";
    }

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

    @GetMapping({ "/admin", "/admin/dashboard", "/admin/dashboard.html" })
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping({ "/admin/domiciliario", "/admin/domiciliario.html" })
    public String adminDomiciliario() {
        return "admin/domiciliario";
    }
}
