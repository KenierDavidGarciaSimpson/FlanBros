package com.flanbross.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flanbross.app.data.Producto
import com.flanbross.app.data.Usuario

@Composable
fun PantallaCatalogo(vm: CatalogoViewModel, onProducto: (Int) -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var categoria by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "FlanBross 🍰",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
        )
        when (val e = estado) {
            is Cargable.Cargando -> Cargando()
            is Cargable.Error -> ErrorReintentar(e.mensaje) { vm.cargar() }
            is Cargable.Listo -> {
                val categorias = e.datos.mapNotNull { it.categoria }.distinct().sorted()
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(selected = categoria == null, onClick = { categoria = null }, label = { Text("Todas") })
                    }
                    items(categorias) { nombre ->
                        FilterChip(selected = categoria == nombre, onClick = { categoria = nombre }, label = { Text(nombre) })
                    }
                }
                val visibles = e.datos.filter { categoria == null || it.categoria == categoria }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibles, key = { it.id }) { producto ->
                        TarjetaProducto(producto) { onProducto(producto.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProducto(producto: Producto, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            ImagenProducto(producto, alto = 110.dp, tamanoEmoji = 48.sp)
            Column(Modifier.padding(10.dp)) {
                producto.badge?.takeIf { it.isNotBlank() }?.let { Etiqueta(it) }
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (producto.stock > 0) Formato.pesos(producto.precio) else "Agotado",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun PantallaProducto(
    id: Int,
    usuario: Usuario?,
    catalogo: CatalogoViewModel,
    carrito: CarritoViewModel,
    onVolver: () -> Unit,
    onIrCarrito: () -> Unit,
    onLogin: () -> Unit,
) {
    val estado by catalogo.estado.collectAsStateWithLifecycle()
    val lineas by carrito.lineas.collectAsStateWithLifecycle()
    val producto = (estado as? Cargable.Listo)?.datos?.firstOrNull { it.id == id }
    var cantidad by rememberSaveable { mutableStateOf(1) }
    var agregado by rememberSaveable { mutableStateOf(false) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }

    if (producto == null) {
        ErrorReintentar("No encontramos este producto.", onVolver)
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ImagenProducto(producto, alto = 240.dp, tamanoEmoji = 96.sp)
        Column(Modifier.padding(16.dp)) {
            TextButton(onClick = onVolver) { Text("← Volver al catálogo") }
            producto.badge?.takeIf { it.isNotBlank() }?.let { Etiqueta(it) }
            Text(producto.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            producto.categoria?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
            Spacer(Modifier.height(8.dp))
            Text(Formato.pesos(producto.precio), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(producto.descripcion.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                if (producto.stock > 0) "Disponibles: ${producto.stock}" else "Agotado por ahora",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            if (producto.stock > 0 && producto.estado == "disponible") {
                if (usuario == null) {
                    Text("Inicia sesión para agregar productos al carrito.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión") }
                } else {
                    val enCarrito = lineas.firstOrNull { it.producto.id == producto.id }?.cantidad ?: 0
                    val libres = (producto.stock - enCarrito).coerceAtLeast(0)
                    if (libres <= 0) {
                        Text(
                            "Ya tienes en el carrito todas las unidades disponibles ($enCarrito).",
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onIrCarrito) { Text("Ver carrito") }
                    } else {
                        if (cantidad > libres) cantidad = libres
                        if (enCarrito > 0) {
                            Text("Ya tienes $enCarrito en el carrito · puedes agregar hasta $libres más", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = { if (cantidad > 1) cantidad-- }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 20.sp) }
                            Text("$cantidad", style = MaterialTheme.typography.titleLarge)
                            OutlinedButton(
                                onClick = {
                                    if (cantidad < libres) cantidad++ else aviso = "Solo hay $libres unidad(es) disponibles"
                                },
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) { Text("+", fontSize = 20.sp) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                aviso = carrito.agregar(producto, cantidad)
                                agregado = aviso == null
                                if (agregado) cantidad = 1
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Agregar al carrito · ${Formato.pesos(producto.precio * cantidad)}") }
                    }
                    aviso?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    if (agregado) {
                        Spacer(Modifier.height(8.dp))
                        Text("✓ Agregado al carrito", color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = onIrCarrito) { Text("Ver carrito") }
                    }
                }
            }
        }
    }
}
