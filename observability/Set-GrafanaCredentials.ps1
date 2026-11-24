[CmdletBinding()]
param(
    [ValidatePattern('^[0-9]+$')]
    [string]$InstanceId = '1936646',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot '.env.grafana.local'
if ((Test-Path -LiteralPath $envPath) -and -not $Force) {
    throw '.env.grafana.local ya existe. Para reemplazarlo explicitamente, usar -Force.'
}
& git -C $repoRoot check-ignore -q -- .env.grafana.local
if ($LASTEXITCODE -ne 0) {
    throw 'El archivo de credenciales no esta ignorado por Git. No se guardo nada.'
}

$secureToken = Read-Host 'Token de envio OTLP (entrada oculta; no se imprime)' -AsSecureString
if ($secureToken.Length -eq 0) { throw 'El token no puede estar vacio.' }
try {
    $credential = [System.Management.Automation.PSCredential]::new($InstanceId, $secureToken)
    $tokenText = $credential.GetNetworkCredential().Password
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($InstanceId + ':' + $tokenText))
    $content = "# Secreto local: la cabecera Basic tambien es una credencial.`n" +
        "GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-sa-east-1.grafana.net/otlp`n" +
        'GRAFANA_OTLP_AUTH="Basic ' + $encoded + '"' + "`n"
    [IO.File]::WriteAllText($envPath, $content, [Text.UTF8Encoding]::new($false))
    Write-Host 'Creado .env.grafana.local. No compartas ni imprimas su contenido.'
} finally {
    $tokenText = $null
    $encoded = $null
    $content = $null
    $credential = $null
    $secureToken.Dispose()
}
