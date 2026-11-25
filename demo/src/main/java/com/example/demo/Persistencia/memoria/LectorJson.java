package com.example.demo.Persistencia.memoria;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * LectorJson — capa 2 (Persistencia), apoyo de las implementaciones en memoria.
 *
 * <p>Lee un archivo de {@code src/main/resources/datos} y lo convierte en una lista
 * de objetos del Modelo. Es el unico punto del proyecto que sabe que los datos
 * vienen de un JSON: cuando se conecte PostgreSQL, esta clase se borra y las
 * implementaciones {@code jpa} no la necesitan.</p>
 *
 * <p>Usa el {@link ObjectMapper} de Spring (el mismo que serializa las respuestas),
 * asi que las fechas ISO de los JSON entran a {@code LocalDate}/{@code LocalDateTime}
 * sin configuracion extra.</p>
 *
 * <p>Capa: Persistencia. No conoce controladores ni vistas.</p>
 */
@Component
public class LectorJson {

    private final ObjectMapper mapeador;

    public LectorJson(ObjectMapper mapeador) {
        this.mapeador = mapeador;
    }

    /**
     * Lee el archivo y devuelve su lista de objetos.
     *
     * @throws IllegalStateException si el archivo no existe o el JSON esta mal; es
     *         preferible que la aplicacion no arranque a que arranque sin catalogo.
     */
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
