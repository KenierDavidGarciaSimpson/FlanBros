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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flanbross.app.data.Pedido
import com.flanbross.app.data.Usuario

/** Historial de pedidos del cliente. El servidor devuelve solo los de la sesion. */
@Composable
fun PantallaPedidos(usuario: Usuario?, vm: PedidosViewModel, onLogin: () -> Unit, onRegistro: () -> Unit) {
    if (usuario == null) {
        PedirSesion("Inicia sesión para ver tus pedidos", onLogin, onRegistro)
        return
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(usuario.idEntidad) { vm.cargar(usuario.idEntidad) }

    Column(Modifier.fillMaxSize()) {
        Text("Mis pedidos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp))
        when (val e = estado) {
            is Cargable.Cargando -> Cargando()
            is Cargable.Error -> ErrorReintentar(e.mensaje) { vm.cargar(usuario.idEntidad) }
            is Cargable.Listo ->
                if (e.datos.isEmpty()) {
                    Text("Todavía no has hecho pedidos.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) { items(e.datos, key = { it.id }) { TarjetaPedido(it) } }
                }
        }
    }
}

@Composable
private fun TarjetaPedido(pedido: Pedido) {
    var abierto by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { abierto = !abierto }) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pedido #${pedido.id}", fontWeight = FontWeight.Bold)
                Etiqueta(Formato.estadoPedido(pedido.estado))
            }
            Text(Formato.fechaHora(pedido.fecha), style = MaterialTheme.typography.bodySmall)
            pedido.numeroFactura?.let { Text("Factura $it", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(4.dp))
            Text(Formato.pesos(pedido.totalFactura ?: pedido.subtotal), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium)
            if (abierto) {
                Spacer(Modifier.height(8.dp))
                pedido.detalles.forEach { linea ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${linea.cantidad} × ${linea.nombre}", modifier = Modifier.weight(1f))
                        Text(Formato.pesos(linea.subtotal))
                    }
                }
            } else {
                Text("Toca para ver el detalle", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
