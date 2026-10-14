#Requires -Version 5.1
<#
.SYNOPSIS
    编译 Forge 客户端模组，并把成品发布到 dist\client-mod\。

.DESCRIPTION
    1. 从 mod-forge\gradle.properties 读出版本号（mod_version）；
    2. 调用 mod-forge\gradlew.bat build；
    3. 把 build\libs\startrail-<版本>.jar 复制成 dist\client-mod\startrail-forge-<版本>.jar；
    4. 刷新 dist\SHA256SUMS.txt。

    版本号只认 gradle.properties 里的 mod_version 一处，改版本请改那里（见 VERSIONING.md）。

.PARAMETER Version
    手动指定版本号（默认读 gradle.properties）。一般不用传。

.PARAMETER Clean
    先执行 gradle clean（全量重编译）。

.PARAMETER Offline
    Gradle 离线模式；依赖都已缓存时更快，没网也能用。

.PARAMETER NoPublish
    只编译，不往 dist\ 复制。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-mod.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-mod.ps1 -Clean -Offline
#>
[CmdletBinding()]
param(
    [string]$Version,
    [switch]$Clean,
    [switch]$Offline,
    [switch]$NoPublish
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Proj = Join-Path $Root 'mod-forge'
$Dist = Join-Path $Root 'dist\client-mod'

if (-not (Test-Path (Join-Path $Proj 'gradlew.bat'))) { throw "找不到 Gradle 工程: $Proj" }

if (-not $Version) {
    $m = Select-String -Path (Join-Path $Proj 'gradle.properties') -Pattern '^\s*mod_version\s*=\s*(.+?)\s*$'
    if (-not $m) { throw 'mod-forge\gradle.properties 里找不到 mod_version' }
    $Version = $m.Matches[0].Groups[1].Value
}
Write-Host "目标版本: $Version" -ForegroundColor Cyan

$tasks = @()
if ($Clean) { $tasks += 'clean' }
$tasks += 'build'
$gradleArgs = $tasks + @('--console=plain')
if ($Offline) { $gradleArgs += '--offline' }

Write-Host ("执行: gradlew.bat {0}" -f ($gradleArgs -join ' ')) -ForegroundColor Cyan
Push-Location $Proj
try {
    & (Join-Path $Proj 'gradlew.bat') @gradleArgs
    $code = $LASTEXITCODE
}
finally {
    Pop-Location
}
if ($code -ne 0) { throw "Gradle 构建失败，退出码 $code" }

$built = Join-Path $Proj ("build\libs\startrail-{0}.jar" -f $Version)
if (-not (Test-Path $built)) { throw "构建产物不存在: $built" }

Write-Host ''
Write-Host ("构建成功: {0}  ({1:N0} 字节)" -f $built, (Get-Item $built).Length) -ForegroundColor Green

if ($NoPublish) { return }

if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Path $Dist -Force | Out-Null }
$target = Join-Path $Dist ("startrail-forge-{0}.jar" -f $Version)
Copy-Item $built $target -Force
Write-Host ("已发布: {0}" -f $target) -ForegroundColor Green

& (Join-Path $PSScriptRoot 'verify-dist.ps1') -Write

Write-Host ''
Write-Host '别忘了：这次改动如果有意义，在 CHANGELOG.md 里记一笔，并跑一次 tools\backup.ps1。' -ForegroundColor Yellow
