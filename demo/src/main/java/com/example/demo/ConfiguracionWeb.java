package com.example.demo;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ConfiguracionWeb — capa 4 (recursos), ajustes de servido de archivos.
 *
 * <p>Las imagenes que sube el panel ({@code POST /api/productos/imagen}) no viven en
 * {@code src/main/resources/static}: se guardan en una carpeta aparte para que no se
 * pisen con el codigo y para que sigan ahi despues de recompilar. Como esa carpeta no
 * esta en el classpath, hay que decirle a Spring que la publique en
 * {@code /img/productos/**}, que es la ruta que piden {@code catalogo.js} y
 * {@code admin-productos.js}.</p>
 *
 * <p>La carpeta se configura con {@code flanbross.carpeta-imagenes} en
 * {@code application.properties}; si no se define, es {@code carpeta-imagenes} al lado
 * del proyecto.</p>
 *
 * <p>Capa: Configuracion. No conoce el Modelo ni la Persistencia.</p>
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final Path carpetaImagenes;

    public ConfiguracionWeb(@Value("${flanbross.carpeta-imagenes:carpeta-imagenes}") String carpetaImagenes) {
        this.carpetaImagenes = Paths.get(carpetaImagenes).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        // La barra final es obligatoria: sin ella Spring interpreta la ruta como archivo.
        String ruta = carpetaImagenes.toUri().toString();
        if (!ruta.endsWith("/")) {
            ruta = ruta + "/";
        }
        registro.addResourceHandler("/img/productos/**").addResourceLocations(ruta);
    }
}
