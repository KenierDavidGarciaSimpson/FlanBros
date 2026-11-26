package com.flanbross.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Rutas {
    const val CATALOGO = "catalogo"
    const val PRODUCTO = "producto/{id}"
    const val CARRITO = "carrito"
    const val CHECKOUT = "checkout"
    const val EXITO = "exito"
    const val PEDIDOS = "pedidos"
    const val CUENTA = "cuenta"
    const val CONTACTO = "contacto"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"

    fun producto(id: Int) = "producto/$id"
}

private data class PestanaInferior(val ruta: String, val emoji: String, val titulo: String)

private val pestanas = listOf(
    PestanaInferior(Rutas.CATALOGO, "🍰", "Catálogo"),
    PestanaInferior(Rutas.CARRITO, "🛒", "Carrito"),
    PestanaInferior(Rutas.PEDIDOS, "📦", "Pedidos"),
    PestanaInferior(Rutas.CUENTA, "👤", "Cuenta"),
)

@Composable
fun Raiz() {
    val sesionVm: SesionViewModel = viewModel()
    val catalogoVm: CatalogoViewModel = viewModel()
    val carritoVm: CarritoViewModel = viewModel()
    val pedidosVm: PedidosViewModel = viewModel()

    val estadoSesion by sesionVm.estado.collectAsStateWithLifecycle()
    val lineas by carritoVm.lineas.collectAsStateWithLifecycle()
    val usuario = (estadoSesion as? EstadoSesion.Activa)?.usuario

    var duenoCarrito by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(usuario?.correo) {
        val actual = usuario?.correo
        if (actual != duenoCarrito) {
            carritoVm.vaciar()
            duenoCarrito = actual
        }
    }

    val nav = rememberNavController()
    val rutaActual = nav.currentBackStackEntryAsState().value?.destination?.route

    if (estadoSesion is EstadoSesion.Verificando) {
        Cargando()
        return
    }

    Scaffold(
        bottomBar = {
            if (pestanas.any { it.ruta == rutaActual }) {
                NavigationBar {
                    pestanas.forEach { pestana ->
                        NavigationBarItem(
                            selected = rutaActual == pestana.ruta,
                            onClick = { irAPestana(nav, pestana.ruta) },
                            icon = {
                                val cantidad = lineas.sumOf { it.cantidad }
                                if (pestana.ruta == Rutas.CARRITO && cantidad > 0) {
                                    BadgedBox(badge = { Badge { Text("$cantidad") } }) { Text(pestana.emoji) }
                                } else {
                                    Text(pestana.emoji)
                                }
                            },
                            label = { Text(pestana.titulo) },
                        )
                    }
                }
            }
        },
    ) { margen ->
        Box(Modifier.fillMaxSize().padding(margen)) {
            NavHost(navController = nav, startDestination = Rutas.CATALOGO) {
                composable(Rutas.CATALOGO) {
                    PantallaCatalogo(catalogoVm) { id -> nav.navigate(Rutas.producto(id)) }
                }
                composable(
                    Rutas.PRODUCTO,
                    arguments = listOf(navArgument("id") { type = NavType.IntType }),
                ) { entrada ->
                    PantallaProducto(
                        id = entrada.arguments?.getInt("id") ?: 0,
                        usuario = usuario,
                        catalogo = catalogoVm,
                        carrito = carritoVm,
                        onVolver = { nav.popBackStack() },
                        onIrCarrito = { irAPestana(nav, Rutas.CARRITO) },
                        onLogin = { nav.navigate(Rutas.LOGIN) },
                    )
                }
                composable(Rutas.CARRITO) {
                    PantallaCarrito(
                        carrito = carritoVm,
                        usuario = usuario,
                        onContinuar = { nav.navigate(if (usuario != null) Rutas.CHECKOUT else Rutas.LOGIN) },
                        onVerCatalogo = { irAPestana(nav, Rutas.CATALOGO) },
                        onLogin = { nav.navigate(Rutas.LOGIN) },
                        onRegistro = { nav.navigate(Rutas.REGISTRO) },
                    )
                }
                composable(Rutas.CHECKOUT) {
                    PantallaCheckout(
                        usuario = usuario,
                        carrito = carritoVm,
                        onVolver = { nav.popBackStack() },
                        onLogin = { nav.navigate(Rutas.LOGIN) },
                        onRegistro = { nav.navigate(Rutas.REGISTRO) },
                        onCreado = { nav.navigate(Rutas.EXITO) { popUpTo(Rutas.CATALOGO) } },
                    )
                }
                composable(Rutas.EXITO) {
                    PantallaExito(
                        carrito = carritoVm,
                        onVerPedidos = { irAPestana(nav, Rutas.PEDIDOS) },
                        onSeguirComprando = { irAPestana(nav, Rutas.CATALOGO) },
                    )
                }
                composable(Rutas.PEDIDOS) {
                    PantallaPedidos(
                        usuario = usuario,
                        vm = pedidosVm,
                        onLogin = { nav.navigate(Rutas.LOGIN) },
                        onRegistro = { nav.navigate(Rutas.REGISTRO) },
                    )
                }
                composable(Rutas.CUENTA) {
                    PantallaCuenta(
                        usuario = usuario,
                        onLogin = { nav.navigate(Rutas.LOGIN) },
                        onRegistro = { nav.navigate(Rutas.REGISTRO) },
                        onContacto = { nav.navigate(Rutas.CONTACTO) },
                        onCerrarSesion = { sesionVm.cerrar() },
                    )
                }
                composable(Rutas.CONTACTO) {
                    PantallaContacto(usuario = usuario, onVolver = { nav.popBackStack() })
                }
                composable(Rutas.LOGIN) {
                    PantallaLogin(
                        sesion = sesionVm,
                        onExito = { nav.popBackStack() },
                        onRegistro = { nav.navigate(Rutas.REGISTRO) },
                        onRecuperar = { nav.navigate(Rutas.RECUPERAR) },
                        onVolver = { nav.popBackStack() },
                    )
                }
                composable(Rutas.REGISTRO) {
                    PantallaRegistro(
                        sesion = sesionVm,
                        onExito = { nav.popBackStack(Rutas.LOGIN, inclusive = true).also { if (!it) nav.popBackStack() } },
                        onVolver = { nav.popBackStack() },
                    )
                }
                composable(Rutas.RECUPERAR) {
                    PantallaRecuperar(onVolver = { nav.popBackStack() })
                }
            }
        }
    }
}

private fun irAPestana(nav: NavHostController, ruta: String) {
    nav.navigate(ruta) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
