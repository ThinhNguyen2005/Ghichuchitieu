[CmdletBinding()]
param(
    [string]$ApiUrl = 'https://api.vietqr.io/v2/banks',
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\app\src\main\assets\vietqr')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-LogoFileName {
    param([string]$Code)

    if ($Code -notmatch '^[A-Za-z0-9_-]+$') {
        throw "Invalid VietQR bank code: $Code"
    }

    return "$Code.png"
}

function Test-PngFile {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    return $bytes.Length -ge 8 -and
        $bytes[0] -eq 137 -and $bytes[1] -eq 80 -and $bytes[2] -eq 78 -and $bytes[3] -eq 71
}

function Save-Logo {
    param(
        [string]$Url,
        [string]$Destination
    )

    $temporaryPath = "$Destination.download"
    try {
        Invoke-WebRequest -Uri $Url -OutFile $temporaryPath -TimeoutSec 30
        if (-not (Test-PngFile -Path $temporaryPath)) {
            throw "Downloaded logo is not a PNG: $Url"
        }
        Move-Item -LiteralPath $temporaryPath -Destination $Destination -Force
    } finally {
        if (Test-Path -LiteralPath $temporaryPath) {
            Remove-Item -LiteralPath $temporaryPath -Force
        }
    }
}

$resolvedOutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$logosDirectory = Join-Path $resolvedOutputDirectory 'logos'
[System.IO.Directory]::CreateDirectory($logosDirectory) | Out-Null

$response = Invoke-RestMethod -Uri $ApiUrl -TimeoutSec 30
if (-not $response.data) {
    throw 'VietQR response did not contain a bank list.'
}

$banks = [System.Collections.Generic.List[object]]::new()
foreach ($bank in $response.data) {
    $code = [string]$bank.code
    $bin = [string]$bank.bin
    if ($bank.transferSupported -ne 1 -or $bin -notmatch '^\d+$') {
        continue
    }

    $logoFileName = Get-LogoFileName -Code $code
    $logoDestination = Join-Path $logosDirectory $logoFileName
    try {
        Save-Logo -Url ([string]$bank.logo) -Destination $logoDestination
    } catch {
        Write-Warning "Skipping $code because its logo could not be saved: $($_.Exception.Message)"
        continue
    }

    $banks.Add([ordered]@{
        id = [int]$bank.id
        bin = $bin
        code = $code
        shortName = [string]$bank.shortName
        name = [string]$bank.name
        logoAssetPath = "vietqr/logos/$logoFileName"
        transferSupported = $true
    })
}

if ($banks.Count -eq 0) {
    throw 'No transfer-supported banks with valid PNG logos were downloaded.'
}

$manifest = [ordered]@{
    schemaVersion = 1
    source = $ApiUrl
    fetchedAtUtc = [DateTime]::UtcNow.ToString('o')
    banks = @($banks | Sort-Object code)
}
$manifestPath = Join-Path $resolvedOutputDirectory 'banks.json'
$json = $manifest | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($manifestPath, $json + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

Write-Output "[OK] Wrote $($banks.Count) banks to $manifestPath"
