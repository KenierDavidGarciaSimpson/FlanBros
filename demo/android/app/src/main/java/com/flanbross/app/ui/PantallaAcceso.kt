package com.flanbross.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flanbross.app.data.Res
import com.flanbross.app.data.Servicios
import com.flanbross.app.data.Usuario
import kotlinx.coroutines.launch

private const val SOLO_CLIENTES = "Esta app es para clientes. El personal usa el panel web."

private suspend fun aceptarSoloClientes(usuario: Usuario): Boolean {
    if (usuario.rol == "cliente") return true
    Servicios.repositorio.cerrarSesion()
    return false
}

@Composable
fun PantallaLogin(
    sesion: SesionViewModel,
    onExito: () -> Unit,
    onRegistro: () -> Unit,
    onRecuperar: () -> Unit,
    onVolver: () -> Unit,
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.align(Alignment.Start)) { Text("← Volver") }
        Text("🍰", fontSize = 56.sp)
        Text("Bienvenido a FlanBross", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        CampoTexto(correo, { correo = it }, "Correo", teclado = KeyboardType.Email)
        Spacer(Modifier.height(8.dp))
        CampoTexto(clave, { clave = it }, "Contraseña", teclado = KeyboardType.Password, secreto = true)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !cargando && correo.isNotBlank() && clave.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = Validacion.correo(correo)
                if (error == null) alcance.launch {
                    cargando = true
                    error = null
                    when (val r = Servicios.repositorio.login(correo.trim(), clave)) {
                        is Res.Ok ->
                            if (aceptarSoloClientes(r.dato)) {
                                sesion.iniciar(r.dato)
                                onExito()
                            } else {
                                error = SOLO_CLIENTES
                            }
                        is Res.Error -> error = r.mensaje
                    }
                    cargando = false
                }
            },
        ) { Text(if (cargando) "Entrando…" else "Iniciar sesión") }
        TextButton(onClick = onRecuperar) { Text("¿Olvidaste tu contraseña?") }
        TextButton(onClick = onRegistro) { Text("¿No tienes cuenta? Regístrate") }
    }
}

@Composable
fun PantallaRegistro(sesion: SesionViewModel, onExito: () -> Unit, onVolver: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellido by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var clave2 by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        CampoTexto(nombre, { nombre = it.take(80) }, "Nombre")
        Spacer(Modifier.height(8.dp))
        CampoTexto(apellido, { apellido = it.take(80) }, "Apellido")
        Spacer(Modifier.height(8.dp))
        CampoTexto(correo, { correo = it.take(120) }, "Correo", teclado = KeyboardType.Email)
        Spacer(Modifier.height(8.dp))
        CampoTexto(telefono, { telefono = it.take(20) }, "Teléfono (opcional)", teclado = KeyboardType.Phone)
        Spacer(Modifier.height(8.dp))
        CampoTexto(direccion, { direccion = it.take(200) }, "Dirección (opcional)")
        Spacer(Modifier.height(8.dp))
        CampoTexto(clave, { clave = it.take(72) }, "Contraseña (mínimo 8 caracteres)", teclado = KeyboardType.Password, secreto = true)
        Spacer(Modifier.height(8.dp))
        CampoTexto(clave2, { clave2 = it.take(72) }, "Confirmar contraseña", teclado = KeyboardType.Password, secreto = true)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !cargando && nombre.isNotBlank() && apellido.isNotBlank() && correo.isNotBlank() && clave.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = Validacion.primero(
                    Validacion.nombre("nombre", nombre),
                    Validacion.nombre("apellido", apellido),
                    Validacion.correo(correo),
                    Validacion.telefono(telefono, obligatorio = false),
                    Validacion.largo("dirección", direccion, 0, 200),
                    Validacion.contrasena(clave),
                    if (clave != clave2) "Las contraseñas no coinciden." else null,
                )
                if (error == null) alcance.launch {
                    cargando = true
                    error = null
                    val r = Servicios.repositorio.registro(
                        nombre.trim(), apellido.trim(), correo.trim(), clave, telefono.trim(), direccion.trim(),
                    )
                    when (r) {
                        is Res.Ok -> { sesion.iniciar(r.dato); onExito() }
                        is Res.Error -> error = r.mensaje
                    }
                    cargando = false
                }
            },
        ) { Text(if (cargando) "Creando…" else "Crear cuenta") }
    }
}

@Composable
fun PantallaRecuperar(onVolver: () -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var nueva by rememberSaveable { mutableStateOf("") }
    var solicitado by rememberSaveable { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var terminado by rememberSaveable { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (terminado) {
            Text("✓ Contraseña actualizada. Ya puedes iniciar sesión.", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Ir a iniciar sesión") }
            return@Column
        }

        CampoTexto(correo, { correo = it }, "Correo de tu cuenta", teclado = KeyboardType.Email)
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = !cargando && correo.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = Validacion.correo(correo)
                if (error == null) alcance.launch {
                    cargando = true
                    error = null
                    when (val r = Servicios.repositorio.solicitarRecuperacion(correo.trim())) {
                        is Res.Ok -> {
                            solicitado = true
                            mensaje = r.mensaje
                            r.dato?.let { token = it }
                        }
                        is Res.Error -> error = r.mensaje
                    }
                    cargando = false
                }
            },
        ) { Text("Enviar instrucciones") }

        if (solicitado) {
            mensaje?.let {
                Spacer(Modifier.height(8.dp))
                Text(it)
            }
            Spacer(Modifier.height(16.dp))
            CampoTexto(token, { token = it }, "Token de recuperación")
            Spacer(Modifier.height(8.dp))
            CampoTexto(nueva, { nueva = it }, "Contraseña nueva (mínimo 8)", teclado = KeyboardType.Password, secreto = true)
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !cargando && token.isNotBlank() && nueva.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    error = Validacion.contrasena(nueva)
                    if (error == null) alcance.launch {
                        cargando = true
                        error = null
                        when (val r = Servicios.repositorio.cambiarContrasena(token.trim(), nueva)) {
                            is Res.Ok -> terminado = true
                            is Res.Error -> error = r.mensaje
                        }
                        cargando = false
                    }
                },
            ) { Text("Cambiar contraseña") }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
