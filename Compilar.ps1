Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  COMPILANDO PROYECTO NETFLIX - Comp. PyD"    -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Crear carpeta de clases si no existe
if (!(Test-Path "build/classes")) {
    New-Item -ItemType Directory -Path "build/classes" -Force | Out-Null
}

Write-Host "[1/3] Compilando clases compartidas..." -ForegroundColor Yellow
javac -d build/classes src/compartido/*.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR en compilacion de compartido." -ForegroundColor Red
    Read-Host "Presione Enter para salir"
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

Write-Host "[2/3] Compilando servidores..." -ForegroundColor Yellow
javac -cp build/classes -d build/classes src/servidores/*.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR en compilacion de servidores." -ForegroundColor Red
    Read-Host "Presione Enter para salir"
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

Write-Host "[3/3] Compilando cliente..." -ForegroundColor Yellow
javac -cp build/classes -d build/classes src/cliente/*.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR en compilacion de cliente." -ForegroundColor Red
    Read-Host "Presione Enter para salir"
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  COMPILACION EXITOSA"                         -ForegroundColor Green
Write-Host "  Ahora ejecute: 2_servidores.ps1"             -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Read-Host "Presione Enter para cerrar"