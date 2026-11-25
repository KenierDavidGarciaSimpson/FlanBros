package com.flanbross.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flanbross.app.data.ItemPedido
import com.flanbross.app.data.NuevoPedido
import com.flanbross.app.data.Res
import com.flanbross.app.data.Servicios
import com.flanbross.app.data.Usuario
import kotlinx.coroutines.launch

/** Carrito: lineas con cantidad, resumen de cobro y paso al checkout. */
@Composable
fun PantallaCarrito(
    carrito: CarritoViewModel,
    usuario: Usuario?,
    onContinuar: () -> Unit,
    onVerCatalogo: () -> Unit,
    onLogin: () -> Unit,
    onRegistro: () -> Unit,
) {
    if (usuario == null) {
        PedirSesion("Inicia sesión para usar el carrito", onLogin, onRegistro)
        return
    }
    val lineas by carrito.lineas.collectAsStateWithLifecycle()
    val cupon by carrito.cupon.collectAsStateWithLifecycle()
    var codigo by rememberSaveable { mutableStateOf("") }
    var avisoCupon by remember { mutableStateOf<String?>(null) }
    var validando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    if (lineas.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🛒", fontSize = 56.sp)
            Text("Tu carrito está vacío", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onVerCatalogo) { Text("Ver el catálogo") }
        }
        return
    }

    val subtotal = carrito.subtotal(lineas)
    Column(Modifier.fillMaxSize()) {
        Text("Tu carrito", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp))
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lineas, key = { it.producto.id }) { linea ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(linea.producto.emoji ?: "🍰", fontSize = 32.sp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(linea.producto.nombre, fontWeight = FontWeight.SemiBold)
                            Text(Formato.pesos(linea.subtotal), color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(onClick = { carrito.cambiarCantidad(linea.producto.id, linea.cantidad - 1) },
                            modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("−") }
                        Text("${linea.cantidad}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        OutlinedButton(
                            enabled = linea.cantidad < linea.producto.stock,
                            onClick = { carrito.cambiarCantidad(linea.producto.id, linea.cantidad + 1) },
                            modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp),
                        ) { Text("+") }
                    }
                }
            }
        }
        // Cupon de descuento
        Column(Modifier.padding(horizontal = 16.dp)) {
            val actual = cupon
            if (actual != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ Cupón ${actual.codigo} (${actual.porcentaje} %)", color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { carrito.aplicarCupon(null); avisoCupon = null }) { Text("Quitar") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        CampoTexto(codigo, { codigo = it.uppercase().take(30) }, "Código de descuento")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = !validando && codigo.isNotBlank(),
                        onClick = {
                            val errorFormato = Validacion.cupon(codigo)
                            avisoCupon = errorFormato
                            if (errorFormato == null) alcance.launch {
                                validando = true
                                avisoCupon = null
                                when (val r = Servicios.repositorio.validarCupon(codigo.trim(), subtotal)) {
                                    is Res.Ok -> { carrito.aplicarCupon(r.dato); codigo = "" }
                                    is Res.Error -> avisoCupon = r.mensaje
                                }
                                validando = false
                            }
                        },
                    ) { Text(if (validando) "…" else "Aplicar") }
                }
            }
            avisoCupon?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        ResumenCobro(subtotal, cupon?.porcentaje ?: 0)
        Button(
            onClick = onContinuar,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text("Continuar con el pedido") }
    }
}

/** Resumen con las mismas reglas del servidor: IVA 8 %; el domicilio se registra aparte. */
@Composable
private fun ResumenCobro(subtotal: Long, porcentaje: Int) {
    val descuento = Cobro.descuento(subtotal, porcentaje)
    val base = subtotal - descuento
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        FilaMonto("Subtotal", Formato.pesos(subtotal))
        if (descuento > 0) FilaMonto("Descuento ($porcentaje %)", "-" + Formato.pesos(descuento))
        FilaMonto("IVA (8 %)", Formato.pesos(Cobro.iva(base)))
        FilaMonto("Total", Formato.pesos(Cobro.total(base)), destacado = true)
        FilaMonto("Domicilio (se paga al recibir)", Formato.pesos(Cobro.COSTO_DOMICILIO))
    }
}

@Composable
private fun FilaMonto(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
        Text(valor, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

private val metodosDePago = listOf(
    "contraentrega" to "Contraentrega",
    "tarjeta" to "Tarjeta",
    "transferencia" to "Transferencia",
    "billetera_digital" to "Billetera digital",
)

/** Checkout: direccion, observaciones y metodo de pago; crea el pedido en el servidor. */
@Composable
fun PantallaCheckout(
    usuario: Usuario?,
    carrito: CarritoViewModel,
    onVolver: () -> Unit,
    onLogin: () -> Unit,
    onRegistro: () -> Unit,
    onCreado: () -> Unit,
) {
    if (usuario == null) {
        PedirSesion("Inicia sesión para confirmar tu pedido", onLogin, onRegistro)
        return
    }
    val lineas by carrito.lineas.collectAsStateWithLifecycle()
    val cupon by carrito.cupon.collectAsStateWithLifecycle()
    var direccion by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var metodo by rememberSaveable { mutableStateOf("contraentrega") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()
    val subtotal = carrito.subtotal(lineas)
    val porcentaje = cupon?.porcentaje ?: 0
    val base = subtotal - Cobro.descuento(subtotal, porcentaje)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver al carrito") }
        Text("Confirmar pedido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        CampoTexto(direccion, { direccion = it.take(200) }, "Dirección de entrega")
        Spacer(Modifier.height(8.dp))
        CampoTexto(telefono, { telefono = it.take(20) }, "Teléfono de contacto")
        Spacer(Modifier.height(8.dp))
        CampoTexto(observaciones, { observaciones = it.take(250) }, "Observaciones (opcional)", lineas = 3)
        Spacer(Modifier.height(12.dp))
        Text("Método de pago", style = MaterialTheme.typography.titleMedium)
        metodosDePago.forEach { (valor, nombre) ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = metodo == valor, onClick = { metodo = valor }, role = Role.RadioButton),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = metodo == valor, onClick = null)
                Text(nombre, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        if (porcentaje > 0) {
            FilaMonto("Cupón ${cupon?.codigo} ($porcentaje %)", "-" + Formato.pesos(Cobro.descuento(subtotal, porcentaje)))
        }
        FilaMonto("Total a pagar", Formato.pesos(Cobro.total(base)), destacado = true)
        FilaMonto("Domicilio (se paga al recibir)", Formato.pesos(Cobro.COSTO_DOMICILIO))
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !enviando && lineas.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = Validacion.primero(
                    Validacion.largo("dirección", direccion, 5, 200),
                    Validacion.telefono(telefono, obligatorio = true),
                    Validacion.largo("observaciones", observaciones, 0, 250),
                )
                if (error == null) alcance.launch {
                    enviando = true
                    // Antes de enviar, confirmar precios y stock con el servidor.
                    val catalogo = Servicios.repositorio.productos()
                    if (catalogo is Res.Ok) {
                        val avisos = carrito.sincronizar(catalogo.dato)
                        if (avisos.isNotEmpty()) {
                            error = avisos.joinToString("\n") + "\nRevisa el carrito antes de confirmar."
                            enviando = false
                            return@launch
                        }
                    }
                    val notas = "Tel: " + telefono.trim() +
                        (observaciones.trim().takeIf { it.isNotEmpty() }?.let { " | $it" } ?: "")
                    val pedido = NuevoPedido(
                        idCliente = usuario.idEntidad,
                        direccionEntrega = direccion.trim(),
                        observaciones = notas,
                        metodoPago = metodo,
                        items = carrito.lineas.value.map { ItemPedido(it.producto.id, it.cantidad, it.producto.precio) },
                        codigoCupon = cupon?.codigo,
                    )
                    when (val r = Servicios.repositorio.crearPedido(pedido)) {
                        is Res.Ok -> { carrito.pedidoConfirmado(r.dato); onCreado() }
                        is Res.Error -> error = r.mensaje
                    }
                    enviando = false
                }
            },
        ) { Text(if (enviando) "Enviando…" else "Confirmar pedido") }
    }
}

/** Pantalla final: confirma el pedido recien creado. */
@Composable
fun PantallaExito(carrito: CarritoViewModel, onVerPedidos: () -> Unit, onSeguirComprando: () -> Unit) {
    val pedido by carrito.ultimoPedido.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎉", fontSize = 64.sp)
        Text("¡Pedido recibido!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        pedido?.let {
            Spacer(Modifier.height(8.dp))
            Text("Pedido #${it.id}", style = MaterialTheme.typography.titleMedium)
            it.numeroFactura?.let { factura -> Text("Factura $factura") }
            Text("Total: ${Formato.pesos(it.total)}", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onVerPedidos, modifier = Modifier.fillMaxWidth()) { Text("Ver mis pedidos") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSeguirComprando, modifier = Modifier.fillMaxWidth()) { Text("Seguir comprando") }
    }
}
