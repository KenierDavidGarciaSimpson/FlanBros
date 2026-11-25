# Arranca FlanBross apuntando a la base de datos remota de Neon.
# Las credenciales se leen de .env (ignorado por git).
$ErrorActionPreference = 'Stop'

$raiz    = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $raiz '.env'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Falta el archivo .env en $raiz"
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $linea = $_.Trim()
    if ($linea -eq '' -or $linea.StartsWith('#')) { return }
    $idx = $linea.IndexOf('=')
    if ($idx -lt 1) { return }
    $clave = $linea.Substring(0, $idx).Trim()
    $valor = $linea.Substring($idx + 1).Trim()
    if (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or
        ($valor.StartsWith("'") -and $valor.EndsWith("'"))) {
        $valor = $valor.Substring(1, $valor.Length - 2)
    }
    Set-Item -Path "Env:$clave" -Value $valor
}

$jar = Join-Path $raiz 'target\demo-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw "No existe $jar. Compila con: .\mvnw.cmd -DskipTests package"
}

Write-Host "Base de datos: $($env:DB_URL)" -ForegroundColor Cyan
& java -jar $jar
