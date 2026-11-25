package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Punto de entrada.
 *
 * <p>Las cuatro capas del proyecto viven todas bajo {@code com.example.demo}:</p>
 * <ul>
 *   <li>{@code Modelo}     - capa 1: clases de dominio, sin Spring.</li>
 *   <li>{@code Persistencia} - capa 2: repositorios (hoy en memoria, manana JPA).</li>
 *   <li>{@code Controlador}  - capa 3: {@code vista/} y {@code api/}.</li>
 *   <li>{@code resources}  - capa 4: vistas ({@code templates/}) y assets ({@code static/}).</li>
 * </ul>
 * Como todo cuelga del paquete raiz, el escaneo por defecto las encuentra sin
 * necesidad de un {@code @ComponentScan} explicito.
 */
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
