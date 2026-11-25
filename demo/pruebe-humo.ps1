# pruebe-humo.ps1 - Prueba de humo de la API (con sesiones, roles y CSRF).
#
# Requisitos: la app corriendo con el perfil por defecto (PostgreSQL + datos demo):
#   docker compose up -d ; .\mvnw.cmd spring-boot:run
# y luego, en otra consola:
#   powershell -ExecutionPolicy Bypass -File .\pruebe-humo.ps1
#
# El script inicia sesion como cliente, administrador, repostero, contador y domiciliarios,
# guarda las cookies de cada uno (WebSession) y envia la cabecera X-XSRF-TOKEN en los POST/PUT/DELETE,
# igual que hace static/js/seguridad.js en el navegador.
#
# Nota: PowerShell 5.1 decodifica las respuestas con la pagina de codigos local; aqui se leen los
# bytes crudos y se decodifican como UTF-8, que es lo que manda el servidor.

$base = 'http://localhost:8080'

$OutputEncoding = [System.Text.Encoding]::UTF8
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$informe = Join-Path $env:TEMP 'humo-flanbross.txt'
Start-Transcript -Path $informe -Force | Out-Null

# ---------------------------------------------------------------- utilidades

function TokenCsrf($sesion) {
    $cookie = $sesion.Cookies.GetCookies([Uri]$base) | Where-Object { $_.Name -eq 'XSRF-TOKEN' } | Select-Object -First 1
    if ($cookie) { return $cookie.Value }
    return $null
}

# $sesion: WebRequestSession (cookies). -SinCsrf: no envia el token. -SinRedireccion: no sigue el 302.
function Peticion($metodo, $ruta, $cuerpo = $null, $sesion = $null, [switch]$SinCsrf, [switch]$SinRedireccion) {
    $parametros = @{
        Uri             = "$base$ruta"
        Method          = $metodo
        UseBasicParsing = $true
    }
    if ($null -ne $sesion) { $parametros.WebSession = $sesion }
    if ($SinRedireccion) { $parametros.MaximumRedirection = 0 }
    if ($null -ne $sesion -and -not $SinCsrf -and $metodo -ne 'GET') {
        $token = TokenCsrf $sesion
        if ($token) { $parametros.Headers = @{ 'X-XSRF-TOKEN' = $token } }
    }
    if ($null -ne $cuerpo) {
        $json = if ($cuerpo -is [string]) { $cuerpo } else { $cuerpo | ConvertTo-Json -Depth 6 -Compress }
        $parametros.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
        $parametros.ContentType = 'application/json; charset=utf-8'
    }

    try {
        $respuesta = Invoke-WebRequest @parametros
        return [pscustomobject]@{
            estado = [int]$respuesta.StatusCode
            texto  = [System.Text.Encoding]::UTF8.GetString($respuesta.RawContentStream.ToArray())
        }
    } catch {
        $respuesta = $_.Exception.Response
        $codigo = if ($respuesta) { [int]$respuesta.StatusCode } else { 0 }
        $texto = ''
        if ($respuesta) {
            try {
                $lector = New-Object System.IO.StreamReader($respuesta.GetResponseStream(), [System.Text.Encoding]::UTF8)
                $texto = $lector.ReadToEnd()
                $lector.Close()
            } catch { }
        }
        return [pscustomobject]@{ estado = $codigo; texto = $texto }
    }
}

# Sesion nueva: visita el login para recibir la cookie XSRF-TOKEN. Sin credenciales = anonima.
function NuevaSesion() {
    $sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    [void](Peticion 'GET' '/login.html' $null $sesion)
    return $sesion
}

function IniciarSesion($correo, $clave) {
    $sesion = NuevaSesion
    $r = Peticion 'POST' '/api/login' @{ correo = $correo; contrasena = $clave } $sesion
    if ($r.estado -ne 200) {
        Write-Host ("[FALLA] No se pudo iniciar sesion como {0} (HTTP {1}): {2}" -f $correo, $r.estado, $r.texto)
    }
    return $sesion
}

function Esperar($titulo, $respuesta, $patron, $estadoEsperado = 200) {
    $cumple = ($respuesta.estado -eq $estadoEsperado) -and ($respuesta.texto -match $patron)
    $marca = if ($cumple) { 'OK   ' } else { 'FALLA' }
    Write-Output ("[{0}] {1} (HTTP {2})" -f $marca, $titulo, $respuesta.estado)
    if (-not $cumple) {
        Write-Output ("       esperaba HTTP {0} y /{1}/" -f $estadoEsperado, $patron)
        Write-Output ("       llego: {0}" -f $respuesta.texto)
    }
}

# La respuesta debe tener el estado esperado y NO contener el patron.
function EsperarSin($titulo, $respuesta, $patronProhibido, $estadoEsperado = 200) {
    $cumple = ($respuesta.estado -eq $estadoEsperado) -and -not ($respuesta.texto -match $patronProhibido)
    $marca = if ($cumple) { 'OK   ' } else { 'FALLA' }
    Write-Output ("[{0}] {1} (HTTP {2})" -f $marca, $titulo, $respuesta.estado)
    if (-not $cumple) {
        Write-Output ("       esperaba HTTP {0} y que NO apareciera /{1}/" -f $estadoEsperado, $patronProhibido)
        Write-Output ("       llego: {0}" -f $respuesta.texto)
    }
}

function Mostrar($titulo, $respuesta) {
    Write-Output "=== $titulo (HTTP $($respuesta.estado)) ==="
    Write-Output $respuesta.texto
}

# ---------------------------------------------------------------- sesiones

Write-Output '--- Inicio de sesion de cada rol ---'
$anonimo   = NuevaSesion
$laura     = IniciarSesion 'laura@demo.com'   'demo1234'    # cliente 1
$admin     = IniciarSesion 'maria@flanbross.co' 'admin1234' # administradora
$repostero = IniciarSesion 'luis@flanbross.co'  'admin1234'
$contador  = IniciarSesion 'ana@flanbross.co'   'admin1234'
$vale      = IniciarSesion 'vale@flanbross.co'  'admin1234' # domiciliaria 3
$felipe    = IniciarSesion 'felipe@flanbross.co' 'admin1234' # domiciliario 4

Write-Output ''
Write-Output '--- Catalogo (publico) ---'
Esperar 'GET /api/productos' (Peticion 'GET' '/api/productos' $null $anonimo) '"exito":true'
Esperar 'GET /api/productos trae acentos y emoji bien' (Peticion 'GET' '/api/productos?id=2' $null $anonimo) 'Cheesecake Maracuy'
Esperar 'GET /api/productos?id=5' (Peticion 'GET' '/api/productos?id=5' $null $anonimo) '"id_producto":5'
Mostrar 'GET /api/productos?categoria=Tortas' (Peticion 'GET' '/api/productos?categoria=Tortas' $null $anonimo)

Write-Output ''
Write-Output '--- Sesion ---'
Esperar 'POST /api/login cliente' (Peticion 'POST' '/api/login' @{ correo = 'laura@demo.com'; contrasena = 'demo1234' } $anonimo) '"nombre_rol":"cliente"'
Esperar 'POST /api/login admin' (Peticion 'POST' '/api/login' @{ correo = 'maria@flanbross.co'; contrasena = 'admin1234' } $anonimo) '"nombre_rol":"administrador"'
Esperar 'POST /api/login con clave mala' (Peticion 'POST' '/api/login' @{ correo = 'laura@demo.com'; contrasena = 'mala' } $anonimo) '"exito":false' 401
Esperar 'POST /api/login sin datos' (Peticion 'POST' '/api/login' @{} $anonimo) '"exito":false' 400
Esperar 'POST /api/recuperar (pide token)' (Peticion 'POST' '/api/recuperar' @{ correo = 'laura@demo.com' } $anonimo) '"exito":true'

Write-Output ''
Write-Output '--- Carrito y checkout (cliente Laura) ---'
Mostrar 'POST /api/cupones DULCE20' (Peticion 'POST' '/api/cupones' @{ codigo = 'DULCE20'; subtotal = 100000 } $laura)
Esperar 'POST /api/cupones inventado' (Peticion 'POST' '/api/cupones' @{ codigo = 'NOEXISTE'; subtotal = 50000 } $laura) '"exito":false' 400

# Se manda id_cliente = 2 a proposito: el servidor debe asignar el pedido a Laura (cliente 1), la de la sesion.
$checkout = @{
    id_cliente        = 2
    direccion_entrega = 'Bocagrande, Calle 5 #2-15'
    observaciones     = 'Prueba de humo'
    metodo_pago       = 'tarjeta'
    items             = @(
        @{ id_producto = 3; cantidad = 2; precio_unitario = 8500 },
        @{ id_producto = 10; cantidad = 1; precio_unitario = 19000 }
    )
}
$pedidoCreado = Peticion 'POST' '/api/pedidos' $checkout $laura
Mostrar 'POST /api/pedidos (checkout)' $pedidoCreado
Esperar 'el checkout devuelve numero_factura' $pedidoCreado '"numero_factura":"FAC-' 201

Esperar 'POST /api/pedidos sin stock' (Peticion 'POST' '/api/pedidos' @{
    id_cliente = 1
    direccion_entrega = 'Calle 1 #2-3'
    items = @(@{ id_producto = 5; cantidad = 999 })
} $laura) '"exito":false' 400

Esperar 'el pedido quedo a nombre de Laura aunque se mando id_cliente=2' (Peticion 'GET' '/api/pedidos?id_cliente=1' $null $laura) 'Prueba de humo'
Esperar 'GET /api/pedidos?id_cliente=1 (historial)' (Peticion 'GET' '/api/pedidos?id_cliente=1' $null $laura) '"numero_factura":"FAC-'
Esperar 'GET /api/facturas (Laura ve las suyas)' (Peticion 'GET' '/api/facturas' $null $laura) '"numero_factura"'
Esperar 'GET /api/pedidos (panel)' (Peticion 'GET' '/api/pedidos' $null $admin) '"exito":true'
Esperar 'PUT /api/pedidos (mover estado)' (Peticion 'PUT' '/api/pedidos' @{ id_pedido = 6; estado = 'en_preparacion' } $admin) '"exito":true'
Esperar 'PUT /api/pedidos con estado inventado' (Peticion 'PUT' '/api/pedidos' @{ id_pedido = 6; estado = 'volando' } $admin) '"exito":false' 400

Write-Output ''
Write-Output '--- Panel (administrador) ---'
Mostrar 'GET /api/dashboard' (Peticion 'GET' '/api/dashboard' $null $admin)
Esperar 'el dashboard usa las claves que lee admin-dashboard.js' (Peticion 'GET' '/api/dashboard' $null $admin) '"ventas_mes".*"pedidos_mes".*"clientes_activos".*"domicilios_activos".*"ultimos_pedidos".*"stock_critico"'
Esperar 'GET /api/inventario' (Peticion 'GET' '/api/inventario' $null $admin) '"cantidad_disponible"'
Esperar 'PUT /api/inventario' (Peticion 'PUT' '/api/inventario' @{ id_producto = 1; cantidad_disponible = 20 } $admin) '"estado_inventario"'
Esperar 'GET /api/facturas' (Peticion 'GET' '/api/facturas' $null $admin) '"numero_factura"'
Esperar 'GET /api/pagos' (Peticion 'GET' '/api/pagos' $null $admin) '"metodo_pago"'
Esperar 'GET /api/domicilios' (Peticion 'GET' '/api/domicilios' $null $admin) '"estado_entrega"'
Esperar 'GET /api/clientes' (Peticion 'GET' '/api/clientes' $null $admin) '"total_pedidos"'
Esperar 'GET /api/empleados' (Peticion 'GET' '/api/empleados' $null $admin) '"nombre_rol"'
Esperar 'PUT /api/clientes (desactivar)' (Peticion 'PUT' '/api/clientes' @{ id_cliente = 5; estado = 'inactivo' } $admin) '"exito":true'
Esperar 'PUT /api/clientes (reactivar)' (Peticion 'PUT' '/api/clientes' @{ id_cliente = 5; estado = 'activo' } $admin) '"exito":true'

Write-Output ''
Write-Output '--- Domiciliarios ---'
Esperar 'GET /api/domicilios?id_domiciliario=3 (Vale)' (Peticion 'GET' '/api/domicilios?id_domiciliario=3' $null $vale) '"exito":true'
EsperarSin 'Vale pide los de Felipe (id 4) y solo recibe los suyos' (Peticion 'GET' '/api/domicilios?id_domiciliario=4' $null $vale) '"id_domiciliario":4'
Esperar 'PUT /api/domicilios por id_pedido (Vale sale a entregar)' (Peticion 'PUT' '/api/domicilios' @{ id_pedido = 6; estado_entrega = 'en_camino'; id_domiciliario = 3 } $vale) '"exito":true'
Esperar 'Felipe no puede tomar el domicilio que ya es de Vale' (Peticion 'PUT' '/api/domicilios' @{ id_pedido = 6; estado_entrega = 'entregado'; id_domiciliario = 4 } $felipe) '"exito":false' 403
Esperar 'PUT /api/domicilios de un pedido sin domicilio' (Peticion 'PUT' '/api/domicilios' @{ id_pedido = 999; estado_entrega = 'en_camino' } $vale) '"exito":false' 404
Esperar 'un domiciliario no puede cancelar pedidos' (Peticion 'PUT' '/api/pedidos' @{ id_pedido = 6; estado = 'cancelado' } $vale) '"exito":false' 403

Write-Output ''
Write-Output '--- CRUD de productos del panel (administrador) ---'
$nuevo = Peticion 'POST' '/api/productos' @{
    nombre_producto = 'Torta de Prueba'
    descripcion     = 'Producto temporal del smoke test'
    precio          = 25000
    stock           = 7
    categoria       = 'Tortas'
    estado_producto = 'disponible'
} $admin
Esperar 'POST /api/productos' $nuevo '"exito":true' 201
$idNuevo = ([regex]::Match($nuevo.texto, '"id_producto":(\d+)')).Groups[1].Value
if (-not $idNuevo) { $idNuevo = 0 }
Esperar 'PUT /api/productos (editar)' (Peticion 'PUT' '/api/productos' @{ id_producto = [int]$idNuevo; precio = 27000 } $admin) '27000'
Esperar 'DELETE /api/productos con id en el cuerpo' (Peticion 'DELETE' '/api/productos' @{ id_producto = [int]$idNuevo } $admin) '"exito":true'
Esperar 'DELETE /api/productos inexistente' (Peticion 'DELETE' '/api/productos' @{ id_producto = 99999 } $admin) '"exito":false' 404

Write-Output ''
Write-Output '--- Empleados del panel (administrador) ---'
$empleadoNuevo = Peticion 'POST' '/api/empleados' @{
    nombre = 'Empleado'; apellido = 'Prueba'; correo = ('empleado.' + [guid]::NewGuid().ToString('N').Substring(0, 8) + '@flanbross.co')
    documento = ([guid]::NewGuid().ToString('N').Substring(0, 12)); telefono = '+57 300 000 0099'; salario = 1800000; id_rol = 5
} $admin
Esperar 'POST /api/empleados' $empleadoNuevo '"exito":true' 201
$idEmpleado = ([regex]::Match($empleadoNuevo.texto, '"id_empleado":(\d+)')).Groups[1].Value
if (-not $idEmpleado) { $idEmpleado = 0 }
Esperar 'PUT /api/empleados (editar)' (Peticion 'PUT' '/api/empleados' @{ id_empleado = [int]$idEmpleado; salario = 1900000 } $admin) '1900000'
Esperar 'DELETE /api/empleados con id en el cuerpo' (Peticion 'DELETE' '/api/empleados' @{ id_empleado = [int]$idEmpleado } $admin) '"exito":true'

Write-Output ''
Write-Output '--- Contacto (publico) ---'
Esperar 'POST /api/contacto' (Peticion 'POST' '/api/contacto' @{
    nombre = 'Ana'; correo = 'ana@correo.com'; telefono = '3001234567'
    asunto = 'Pedido especial'; mensaje = 'Necesito una torta para el sabado'
} $anonimo) '"exito":true' 201

# ---------------------------------------------------------------- SEGURIDAD

Write-Output ''
Write-Output '--- Seguridad: sin sesion ---'
Esperar 'GET /api/pagos sin sesion' (Peticion 'GET' '/api/pagos' $null $anonimo) '.*' 401
Esperar 'GET /api/clientes sin sesion' (Peticion 'GET' '/api/clientes' $null $anonimo) '.*' 401
Esperar 'GET /api/pedidos sin sesion' (Peticion 'GET' '/api/pedidos' $null $anonimo) '.*' 401
Esperar 'POST /api/pedidos sin sesion' (Peticion 'POST' '/api/pedidos' $checkout $anonimo) '.*' 401
Esperar '/admin/dashboard.html sin sesion redirige al login' (Peticion 'GET' '/admin/dashboard.html' $null $anonimo -SinRedireccion) '.*' 302
Esperar '/cuenta.html sin sesion redirige al login' (Peticion 'GET' '/cuenta.html' $null $anonimo -SinRedireccion) '.*' 302

Write-Output ''
Write-Output '--- Seguridad: roles ---'
Esperar 'cliente no puede ver pagos' (Peticion 'GET' '/api/pagos' $null $laura) '.*' 403
Esperar 'cliente no puede ver empleados' (Peticion 'GET' '/api/empleados' $null $laura) '.*' 403
Esperar 'cliente no puede cambiar el inventario' (Peticion 'PUT' '/api/inventario' @{ id_producto = 1; cantidad_disponible = 99 } $laura) '.*' 403
Esperar 'contador puede ver pagos' (Peticion 'GET' '/api/pagos' $null $contador) '"metodo_pago"'
Esperar 'contador no puede ver empleados' (Peticion 'GET' '/api/empleados' $null $contador) '.*' 403
Esperar 'repostero puede ver el inventario' (Peticion 'GET' '/api/inventario' $null $repostero) '"cantidad_disponible"'
Esperar 'repostero no puede ver domicilios' (Peticion 'GET' '/api/domicilios' $null $repostero) '.*' 403
Esperar 'domiciliario no puede ver clientes' (Peticion 'GET' '/api/clientes' $null $vale) '.*' 403

Write-Output ''
Write-Output '--- Seguridad: cada quien ve solo lo suyo ---'
EsperarSin 'Laura pide pedidos del cliente 2 y solo recibe los suyos' (Peticion 'GET' '/api/pedidos?id_cliente=2' $null $laura) '"id_cliente":2'
Esperar 'Laura no puede abrir el pedido 6 (es de otra clienta)' (Peticion 'GET' '/api/pedidos?id=6' $null $laura) '"exito":false'

Write-Output ''
Write-Output '--- Seguridad: CSRF ---'
Esperar 'POST /api/contacto sin token CSRF se rechaza' (Peticion 'POST' '/api/contacto' @{
    nombre = 'X'; correo = 'x@x.com'; asunto = 'Otro motivo'; mensaje = 'sin token'
} $anonimo -SinCsrf) '.*' 403
Esperar 'PUT /api/inventario sin token CSRF se rechaza (aun siendo admin)' (Peticion 'PUT' '/api/inventario' @{ id_producto = 1; cantidad_disponible = 20 } $admin -SinCsrf) '.*' 403

Write-Output ''
Write-Output '--- Seguridad: login ---'
$intruso = 'intruso-' + [guid]::NewGuid().ToString('N').Substring(0, 8) + '@demo.com'
for ($i = 1; $i -le 5; $i++) {
    Esperar "intento fallido $i/5" (Peticion 'POST' '/api/login' @{ correo = $intruso; contrasena = 'x' } $anonimo) '"exito":false' 401
}
Esperar 'el intento 6 queda bloqueado (429)' (Peticion 'POST' '/api/login' @{ correo = $intruso; contrasena = 'x' } $anonimo) '"exito":false' 429

Esperar 'desactivar a Juliana (cliente 2)' (Peticion 'PUT' '/api/clientes' @{ id_cliente = 2; estado = 'inactivo' } $admin) '"exito":true'
Esperar 'una cuenta inactiva no puede iniciar sesion' (Peticion 'POST' '/api/login' @{ correo = 'juliana@demo.com'; contrasena = 'demo1234' } $anonimo) '"exito":false' 401
Esperar 'reactivar a Juliana' (Peticion 'PUT' '/api/clientes' @{ id_cliente = 2; estado = 'activo' } $admin) '"exito":true'
Esperar 'Juliana vuelve a entrar' (Peticion 'POST' '/api/login' @{ correo = 'juliana@demo.com'; contrasena = 'demo1234' } $anonimo) '"nombre_rol":"cliente"'

$temporal = IniciarSesion 'alex@demo.com' 'demo1234'
Esperar 'sesion de Alex activa' (Peticion 'GET' '/api/pedidos?id_cliente=3' $null $temporal) '"exito":true'
Esperar 'POST /api/logout' (Peticion 'POST' '/api/logout' $null $temporal) '.*' 204
Esperar 'tras cerrar sesion la API responde 401' (Peticion 'GET' '/api/pedidos?id_cliente=3' $null $temporal) '.*' 401

# ---------------------------------------------------------------- vistas y estaticos

Write-Output ''
Write-Output '--- Vistas (Thymeleaf) ---'
$paginasPublicas = @(
    '/', '/index.html', '/catalogo.html', '/pages/producto.html?id=1',
    '/carrito.html', '/checkout.html', '/contacto.html',
    '/nosotros.html', '/login.html', '/registro.html', '/recuperar.html'
)
foreach ($pagina in $paginasPublicas) {
    Esperar "GET $pagina" (Peticion 'GET' $pagina $null $anonimo) '<!DOCTYPE html>'
}
Esperar 'GET /cuenta.html (cliente con sesion)' (Peticion 'GET' '/cuenta.html' $null $laura) '<!DOCTYPE html>'
Esperar 'GET /admin/dashboard.html (admin)' (Peticion 'GET' '/admin/dashboard.html' $null $admin) '<!DOCTYPE html>'
Esperar 'GET /admin/domiciliario.html (domiciliaria)' (Peticion 'GET' '/admin/domiciliario.html' $null $vale) '<!DOCTYPE html>'
Esperar 'un cliente no entra a /admin/dashboard.html' (Peticion 'GET' '/admin/dashboard.html' $null $laura -SinRedireccion) '.*' 302

Write-Output ''
Write-Output '--- Archivos estaticos (publicos) ---'
Esperar 'GET /css/estilos.css' (Peticion 'GET' '/css/estilos.css' $null $anonimo) 'contenido-pagina|@import|--'
Esperar 'GET /js/modules/utils.js' (Peticion 'GET' '/js/modules/utils.js' $null $anonimo) "const API = '/api'"
Esperar 'GET /js/seguridad.js' (Peticion 'GET' '/js/seguridad.js' $null $anonimo) 'X-XSRF-TOKEN'
Esperar 'GET /admin/js/admin-nav.js' (Peticion 'GET' '/admin/js/admin-nav.js' $null $anonimo) 'mostrarSeccion'

Write-Output ''
Write-Output '--- Subida de imagen (multipart, administrador) ---'
$imagen = Join-Path $env:TEMP 'prueba-smoke.png'
# PNG 1x1 minimo: basta para comprobar que el archivo llega y se guarda.
[System.IO.File]::WriteAllBytes($imagen, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AAAwAB/wD/9ZcAAAAASUVORK5CYII='))
$galletas = ($admin.Cookies.GetCookies([Uri]$base) | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join '; '
$tokenAdmin = TokenCsrf $admin
$subida = & curl.exe -s -X POST -H "X-XSRF-TOKEN: $tokenAdmin" -H "Cookie: $galletas" -F "imagen=@$imagen" "$base/api/productos/imagen"
Esperar 'POST /api/productos/imagen' ([pscustomobject]@{ estado = 200; texto = $subida }) '"nombre_archivo"'
$nombreImagen = ([regex]::Match($subida, '"nombre_archivo":"([^"]+)"')).Groups[1].Value
if ($nombreImagen) {
    Esperar "GET /img/productos/$nombreImagen" (Peticion 'GET' "/img/productos/$nombreImagen" $null $anonimo) '.' 200
}
Remove-Item $imagen -Force -ErrorAction SilentlyContinue

Write-Output ''
Write-Output '--- Fin de la prueba de humo ---'
Stop-Transcript | Out-Null
Write-Output "Informe completo en: $informe"
