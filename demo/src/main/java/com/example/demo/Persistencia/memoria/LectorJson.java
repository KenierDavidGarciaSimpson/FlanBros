package com.example.demo.Persistencia.memoria;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJson {

    private final ObjectMapper mapeador;

    public LectorJson(ObjectMapper mapeador) {
        this.mapeador = mapeador;
    }

    public <T> List<T> leerLista(String ruta, Class<T> tipo) {
        ClassPathResource recurso = new ClassPathResource(ruta);
        if (!recurso.exists()) {
            throw new IllegalStateException("Falta el archivo de datos '" + ruta
                    + "' en src/main/resources. Sin el, la aplicacion no tiene que mostrar.");
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return mapeador.readValue(entrada,
                    mapeador.getTypeFactory().constructCollectionType(List.class, tipo));
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo leer '" + ruta + "': " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir '" + ruta + "': " + e.getMessage(), e);
        }
    }
}
