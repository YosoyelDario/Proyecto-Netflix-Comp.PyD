# ============================================================
#  SCRIPT 3 - INICIAR CLIENTE
#  Cada ejecucion abre un cliente con una IP distinta
#  Ejecute varias veces para simular multiples usuarios
# ============================================================

$ruta = (Get-Location).Path

# Archivo temporal para llevar el contador de clientes
$archivoContador = "$ruta\.cliente_contador"

# Leer o inicializar el contador
if (Test-Path $archivoContador) {
    $contador = [int](Get-Content $archivoContador)
    $contador++
} else {
    $contador = 1
}

# Guardar nuevo valor
$contador | Out-File $archivoContador -Force

# Generar IP unica para este cliente (127.0.0.X)
# Empieza en 127.0.0.10 para evitar conflictos
$ultimoOcteto = 9 + $contador
if ($ultimoOcteto -gt 254) { $ultimoOcteto = 10; $contador = 1; $contador | Out-File $archivoContador -Force }
$ipCliente = "127.0.0.$ultimoOcteto"

# Colores distintos por cliente
$colores = @("Cyan", "Yellow", "Magenta", "White", "Green")
$color = $colores[($contador - 1) % $colores.Count]

Write-Host ""
Write-Host "=============================================" -ForegroundColor $color
Write-Host "  Iniciando Cliente #$contador"                -ForegroundColor $color
Write-Host "  IP simulada: $ipCliente"                     -ForegroundColor $color
Write-Host "=============================================" -ForegroundColor $color
Write-Host ""

# Abrir nueva ventana con el cliente
Start-Process powershell -ArgumentList "-NoExit", "-Command", "
    Set-Location '$ruta'
    `$Host.UI.RawUI.WindowTitle = 'CLIENTE #$contador - IP: $ipCliente'
    Write-Host '========================================' -ForegroundColor $color
    Write-Host '  CLIENTE NETFLIX #$contador' -ForegroundColor $color
    Write-Host '  IP simulada: $ipCliente' -ForegroundColor $color
    Write-Host '  Gateway: localhost:4000 (TCP/SSL)' -ForegroundColor $color
    Write-Host '  Streaming: localhost:6000 (UDP)' -ForegroundColor $color
    Write-Host '========================================' -ForegroundColor $color
    Write-Host ''
    java -cp build/classes cliente.ClienteInteractivo localhost 4000 localhost 6000 $ipCliente
"

Write-Host "Cliente #$contador abierto en nueva ventana." -ForegroundColor Green
Write-Host "Ejecute este script de nuevo para otro cliente." -ForegroundColor Gray
Write-Host ""