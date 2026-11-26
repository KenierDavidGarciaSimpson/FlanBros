package com.example.demo.Controlador.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Controlador.util.PeticionUtil;
import com.example.demo.Modelo.Producto;
import com.example.demo.Modelo.dto.RespuestaApi;
import com.example.demo.Persistencia.ProductoRepositorio;

@RestController
@RequestMapping("/api/productos")
public class ProductosApiControlador {

    private final ProductoRepositorio productos;

    private final Path carpetaImagenes;

    public ProductosApiControlador(ProductoRepositorio productos,
                                   @Value("${flanbross.carpeta-imagenes:carpeta-imagenes}") String carpetaImagenes) {
        this.productos = productos;
        this.carpetaImagenes = Paths.get(carpetaImagenes).toAbsolutePath().normalize();
    }

    @GetMapping
    public RespuestaApi listar(@RequestParam(required = false) Integer id,
                               @RequestParam(required = false) String categoria) {
        if (id != null) {
            return productos.buscarPorId(id)
                    .<RespuestaApi>map(RespuestaApi::ok)
                    .orElseGet(() -> RespuestaApi.error("Producto no encontrado"));
        }
        if (categoria != null && !categoria.isBlank()) {
            return RespuestaApi.ok(productos.listarPorCategoria(categoria));
        }
        return RespuestaApi.ok(productos.listarDisponibles());
    }

    @PostMapping
    public ResponseEntity<RespuestaApi> crear(@RequestBody Map<String, Object> cuerpo) {
        String nombre = PeticionUtil.texto(cuerpo, "nombre_producto");
        BigDecimal precio = PeticionUtil.decimal(cuerpo, "precio");
        if (nombre == null || precio == null) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Faltan datos obligatorios: nombre_producto y precio"));
        }

        Producto producto = new Producto();
        producto.nombre_producto = nombre;
        producto.descripcion = PeticionUtil.texto(cuerpo, "descripcion");
        producto.precio = precio;
        producto.stock = Math.max(PeticionUtil.entero(cuerpo, "stock"), 0);
        producto.categoria = PeticionUtil.texto(cuerpo, "categoria", "Otros");
        producto.imagen = PeticionUtil.texto(cuerpo, "imagen");
        producto.emoji = PeticionUtil.texto(cuerpo, "emoji", "🍰");
        producto.badge = PeticionUtil.texto(cuerpo, "badge");
        producto.estado_producto = "disponible";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.ok(productos.crear(producto), "Producto creado correctamente"));
    }

    @PutMapping
    public ResponseEntity<RespuestaApi> actualizar(@RequestBody Map<String, Object> cuerpo) {
        int id = PeticionUtil.entero(cuerpo, "id_producto");
        Optional<Producto> encontrado = productos.buscarPorId(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Producto no encontrado"));
        }
        Producto actual = encontrado.get();

        Producto cambios = new Producto();
        cambios.id_producto = id;
        cambios.nombre_producto = PeticionUtil.texto(cuerpo, "nombre_producto", actual.nombre_producto);
        cambios.descripcion = PeticionUtil.texto(cuerpo, "descripcion", actual.descripcion);
        BigDecimal precio = PeticionUtil.decimal(cuerpo, "precio");
        cambios.precio = precio == null ? actual.precio : precio;
        cambios.stock = cuerpo.containsKey("stock")
                ? Math.max(PeticionUtil.entero(cuerpo, "stock"), 0) : actual.stock;
        cambios.categoria = PeticionUtil.texto(cuerpo, "categoria", actual.categoria);
        cambios.imagen = PeticionUtil.texto(cuerpo, "imagen", actual.imagen);
        cambios.emoji = PeticionUtil.texto(cuerpo, "emoji", actual.emoji);
        cambios.badge = PeticionUtil.texto(cuerpo, "badge", actual.badge);
        cambios.estado_producto = PeticionUtil.texto(cuerpo, "estado_producto", actual.estado_producto);

        if (!productos.actualizar(cambios)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Producto no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(productos.buscarPorId(id).orElse(cambios),
                "Producto actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApi> desactivar(@PathVariable int id) {
        return desactivarPorId(id);
    }

    @DeleteMapping
    public ResponseEntity<RespuestaApi> desactivarPorParametro(
            @RequestParam(required = false) Integer id,
            @RequestBody(required = false) Map<String, Object> cuerpo) {

        int idProducto = id != null ? id : PeticionUtil.entero(cuerpo, "id_producto");
        if (idProducto <= 0) {
            return ResponseEntity.badRequest()
                    .body(RespuestaApi.error("Falta el id del producto"));
        }
        return desactivarPorId(idProducto);
    }

    private ResponseEntity<RespuestaApi> desactivarPorId(int id) {
        if (!productos.desactivar(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RespuestaApi.error("Producto no encontrado"));
        }
        return ResponseEntity.ok(RespuestaApi.ok(productos.buscarPorId(id).orElse(null),
                "Producto desactivado correctamente"));
    }

    private static final List<String> EXTENSIONES = List.of("jpg", "jpeg", "png", "webp", "gif");

    @PostMapping("/imagen")
    public ResponseEntity<RespuestaApi> subirImagen(@RequestParam("imagen") MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(RespuestaApi.error("No llego ninguna imagen"));
        }

        String nombre = nombreArchivoSeguro(archivo.getOriginalFilename());
        if (!EXTENSIONES.contains(extensionDe(nombre))) {
            return ResponseEntity.badRequest().body(RespuestaApi.error(
                    "Formato no admitido: usa jpg, jpeg, png, webp o gif"));
        }

        try {
            Files.createDirectories(carpetaImagenes);
            archivo.transferTo(carpetaImagenes.resolve(nombre));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RespuestaApi.error("No se pudo guardar la imagen: " + e.getMessage()));
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre_archivo", nombre);
        return ResponseEntity.ok(RespuestaApi.ok(datos, "Imagen subida correctamente"));
    }

    private static String nombreArchivoSeguro(String original) {
        String base = original == null ? "" : original.replaceAll("[^A-Za-z0-9._-]", "-");
        base = base.replaceAll("^[.-]+", "");
        if (base.isBlank()) {
            base = "imagen.jpg";
        }
        return System.currentTimeMillis() + "-" + base;
    }

    private static String extensionDe(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
