# ============================================================
#  SCRIPT 2 - INICIAR TODOS LOS SERVIDORES
#  Abre 5 ventanas de PowerShell, una por cada servidor
#  Orden de inicio: Auth -> Catalogo -> Subtitulos -> Streaming -> Gateway
# ============================================================

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  INICIANDO SERVIDORES NETFLIX"                -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que este compilado
if (!(Test-Path "build/classes/servidores/GatewayZuul.class")) {
    Write-Host "ERROR: No se encontraron clases compiladas." -ForegroundColor Red
    Write-Host "Ejecute primero: 1_compilar.ps1" -ForegroundColor Yellow
    Read-Host "Presione Enter para salir"
    exit 1
}

$ruta = (Get-Location).Path

# --- Servidor B: Autenticacion (Puerto 5100) ---
Write-Host "[1/5] Servidor B - Autenticacion (puerto 5100)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "
    Set-Location '$ruta'
    `$Host.UI.RawUI.WindowTitle = 'SERVIDOR B - Autenticacion (5100)'
    Write-Host '========================================' -ForegroundColor Magenta
    Write-Host '  SERVIDOR B - Autenticacion' -ForegroundColor Magenta
    Write-Host '  Puerto: 5100' -ForegroundColor Magenta
    Write-Host '========================================' -ForegroundColor Magenta
    java -cp build/classes servidores.ServidorAutenticacion
"
Start-Sleep -Seconds 2

# --- Servidor A: Catalogo (Puerto 5000) ---
Write-Host "[2/5] Servidor A - Catalogo (puerto 5000)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "
    Set-Location '$ruta'
    `$Host.UI.RawUI.WindowTitle = 'SERVIDOR A - Catalogo (5000)'
    Write-Host '========================================' -ForegroundColor Blue
    Write-Host '  SERVIDOR A - Catalogo y Busqueda' -ForegroundColor Blue
    Write-Host '  Puerto: 5000' -ForegroundColor Blue
    Write-Host '========================================' -ForegroundColor Blue
    java -cp build/classes servidores.ServidorCatalogo
"
Start-Sleep -Seconds 2

# --- Servidor C: Subtitulos (Puerto 7000) ---
Write-Host "[3/5] Servidor C - Subtitulos (puerto 7000)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "
    Set-Location '$ruta'
    `$Host.UI.RawUI.WindowTitle = 'SERVIDOR C - Subtitulos (7000)'
    Write-Host '========================================' -ForegroundColor DarkYellow
    Write-Host '  SERVIDOR C - Subtitulos' -ForegroundColor DarkYellow
    Write-Host '  Puerto: 7000' -ForegroundColor DarkYellow
    Write-Host '========================================' -ForegroundColor DarkYellow
    java -cp build/classes servidores.ServidorSubtitulos
"
Start-Sleep -Seconds 2

# --- Servidor Streaming UDP (Puerto 6000) ---
Write-Host "[4/5] Servidor Streaming UDP (puerto 6000)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "
    Set-Location '$ruta'
    `$Host.UI.RawUI.WindowTitle = 'SERVIDOR STREAMING - UDP (6000)'
    Write-Host '========================================' -ForegroundColor Red
    Write-Host '  SERVIDOR STREAMING UDP' -ForegroundColor Red
    Write-Host '  Puerto: 6000' -ForegroundColor Red
    Write-Host '========================================' -ForegroundColor Red
    java -cp build/classes servidores.ServidorStreamingUDP
"
Start-Sleep -Seconds 2

# --- Gateway ZUUL (Puerto 4000) - Se inicia ultimo ---
Write-Host "[5/5] Gateway ZUUL (puerto 4000)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "
    Set-Location '$ruta'
    `$Host.UI.RawUI.WindowTitle = 'GATEWAY ZUUL (4000)'
    Write-Host '========================================' -ForegroundColor Green
    Write-Host '  GATEWAY ZUUL - Punto de Entrada' -ForegroundColor Green
    Write-Host '  Puerto: 4000' -ForegroundColor Green
    Write-Host '========================================' -ForegroundColor Green
    java -cp build/classes servidores.GatewayZuul
"
Start-Sleep -Seconds 1

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  5 SERVIDORES INICIADOS"                      -ForegroundColor Green
Write-Host ""                                               
Write-Host "  Gateway ZUUL ......... puerto 4000"          -ForegroundColor White
Write-Host "  Servidor A (Catalogo)  puerto 5000"          -ForegroundColor White
Write-Host "  Servidor B (Auth) .... puerto 5100"          -ForegroundColor White
Write-Host "  Streaming UDP ........ puerto 6000"          -ForegroundColor White
Write-Host "  Servidor C (Subs) ... puerto 7000"           -ForegroundColor White
Write-Host ""
Write-Host "  Ahora ejecute: 3_cliente.ps1"                -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Read-Host "Presione Enter para cerrar esta ventana"