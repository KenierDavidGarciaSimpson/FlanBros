package com.example.demo;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final Path carpetaImagenes;

    public ConfiguracionWeb(@Value("${flanbross.carpeta-imagenes:carpeta-imagenes}") String carpetaImagenes) {
        this.carpetaImagenes = Paths.get(carpetaImagenes).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String ruta = carpetaImagenes.toUri().toString();
        if (!ruta.endsWith("/")) {
            ruta = ruta + "/";
        }
        registro.addResourceHandler("/img/productos/**").addResourceLocations(ruta);
    }
}
