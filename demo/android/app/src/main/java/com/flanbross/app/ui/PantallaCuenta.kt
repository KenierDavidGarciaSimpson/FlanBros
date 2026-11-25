package com.flanbross.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flanbross.app.data.Res
import com.flanbross.app.data.Servicios
import com.flanbross.app.data.Usuario
import kotlinx.coroutines.launch

@Composable
fun PantallaCuenta(
    usuario: Usuario?,
    onLogin: () -> Unit,
    onRegistro: () -> Unit,
    onContacto: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    if (usuario == null) {
        Column(Modifier.fillMaxSize()) {
            PedirSesion("Inicia sesión para ver tu cuenta", onLogin, onRegistro)
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Mi cuenta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("${usuario.nombre} ${usuario.apellido}".trim(), style = MaterialTheme.typography.titleLarge)
        Text(usuario.correo, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onContacto, modifier = Modifier.fillMaxWidth()) { Text("Contáctanos") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}

/**
 * Formulario de contacto. Requiere sesion: el servidor toma nombre, correo y telefono
 * de la cuenta, asi que aqui solo se escribe el mensaje (igual que en la web).
 */
@Composable
fun PantallaContacto(usuario: Usuario?, onVolver: () -> Unit) {
    var mensaje by rememberSaveable { mutableStateOf("") }
    var respuesta by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Contáctanos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (usuario == null) {
            Text("Inicia sesión para enviarnos un mensaje.")
            return@Column
        }
        Text(
            "Enviando como ${"${usuario.nombre} ${usuario.apellido}".trim()} (${usuario.correo})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(8.dp))
        CampoTexto(mensaje, { mensaje = it.take(2000) }, "Mensaje (mínimo 10 caracteres)", lineas = 5)
        respuesta?.let {
            Spacer(Modifier.height(8.dp))
            Text("✓ $it", color = MaterialTheme.colorScheme.primary)
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !enviando && mensaje.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                respuesta = null
                error = Validacion.largo("mensaje", mensaje, 10, 2000)
                if (error == null) alcance.launch {
                    enviando = true
                    val nombre = "${usuario.nombre} ${usuario.apellido}".trim()
                    when (val r = Servicios.repositorio.enviarContacto(nombre, usuario.correo, null, "Mensaje desde la app", mensaje.trim())) {
                        is Res.Ok -> { respuesta = r.mensaje; mensaje = "" }
                        is Res.Error -> error = r.mensaje
                    }
                    enviando = false
                }
            },
        ) { Text(if (enviando) "Enviando…" else "Enviar mensaje") }
    }
}
