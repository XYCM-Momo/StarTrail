#Requires -Version 5.1
<#
.SYNOPSIS
    编译 Paper 服务端插件，并把成品发布到 dist\server-plugin\。

.DESCRIPTION
    1. 从 plugin-paper\src\main\resources\plugin.yml 读出版本号；
    2. 调用 plugin-paper\gradlew.bat build；
    3. 把 build\libs\StarTrail-Plugin-<版本>.jar 复制到 dist\server-plugin\；
    4. 刷新 dist\SHA256SUMS.txt。

    版本号只认 plugin.yml 的 version 一处（见 VERSIONING.md）。

.PARAMETER Version
    手动指定版本号（默认读 plugin.yml）。一般不用传。

.PARAMETER Clean
    先执行 gradle clean（全量重编译）。

.PARAMETER Offline
    Gradle 离线模式。

.PARAMETER NoPublish
    只编译，不往 dist\ 复制。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-plugin.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-plugin.ps1 -Clean -Offline
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
$Proj = Join-Path $Root 'plugin-paper'
$Dist = Join-Path $Root 'dist\server-plugin'

if (-not (Test-Path (Join-Path $Proj 'gradlew.bat'))) { throw "找不到 Gradle 工程: $Proj" }

$pluginYml = Join-Path $Proj 'src\main\resources\plugin.yml'
if (-not $Version) {
    $m = Select-String -Path $pluginYml -Pattern "^version:\s*['""]?([^'""\s]+)['""]?\s*$"
    if (-not $m) { throw "plugin.yml 里找不到 version" }
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

$built = Join-Path $Proj ("build\libs\StarTrail-Plugin-{0}.jar" -f $Version)
if (-not (Test-Path $built)) { throw "构建产物不存在: $built" }

Write-Host ''
Write-Host ("构建成功: {0}  ({1:N0} 字节)" -f $built, (Get-Item $built).Length) -ForegroundColor Green

if ($NoPublish) { return }

if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Path $Dist -Force | Out-Null }
$target = Join-Path $Dist ("StarTrail-Plugin-{0}.jar" -f $Version)
Copy-Item $built $target -Force
Write-Host ("已发布: {0}" -f $target) -ForegroundColor Green

& (Join-Path $PSScriptRoot 'verify-dist.ps1') -Write

Write-Host ''
Write-Host '部署：把 dist\server-plugin 里的 jar 复制到服务端 plugins\ 覆盖旧文件，然后重启服务端（或 /reload 不推荐）。' -ForegroundColor Yellow
Write-Host '改过插件逻辑的话，记得在 CHANGELOG.md 记一笔并跑一次 tools\backup.ps1。' -ForegroundColor Yellow
