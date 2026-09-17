#Requires -Version 5.1
<#
.SYNOPSIS
    用一次性 Paper 服务器冒烟测试服务端插件（不碰线上服务端）。

.DESCRIPTION
    会在临时目录里复制一份 Paper 服务端、装上指定的插件 jar、启动、等它加载完成，
    然后检查插件有没有正常启用、日志里有没有异常，最后把服务器关掉。

    用途：改完插件、往线上服务端放之前先跑一遍。放上去崩了可比这麻烦多了。

.PARAMETER PluginJar
    要测试的插件 jar。默认取 dist\server-plugin\ 里最新的 MMmap-Plugin-*.jar。

.PARAMETER PaperJar
    Paper 服务端 jar，默认用线上服务端那份（E:\Minecraft\服务器\时旅服务端\Paper-1.20.1.jar）。

.PARAMETER JavaExe
    java.exe 路径。默认用线上服务端同款 JDK 21；找不到就退回 PATH 里的 java。

.PARAMETER Port
    测试服端口，默认 25599（随便挑个没人用的，别和线上 25565 撞）。

.PARAMETER TimeoutSeconds
    等启动的最长秒数，默认 300。

.PARAMETER WithMythicMobs
    把线上服务端的 MythicMobs 也放进测试服，用来测 MythicMobs 集成那条路径。

.PARAMETER KeepServer
    测完不删临时目录（方便自己翻日志）。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\smoke-test-plugin.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\smoke-test-plugin.ps1 -WithMythicMobs -KeepServer
#>
[CmdletBinding()]
param(
    [string]$PluginJar,
    [string]$PaperJar = 'E:\Minecraft\服务器\时旅服务端\Paper-1.20.1.jar',
    [string]$JavaExe  = 'C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe',
    [int]$Port = 25599,
    [int]$TimeoutSeconds = 300,
    [switch]$WithMythicMobs,
    [switch]$KeepServer
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot

if (-not $PluginJar) {
    $cand = Get-ChildItem (Join-Path $Root 'dist\server-plugin') -Filter 'MMmap-Plugin-*.jar' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $cand) { throw 'dist\server-plugin 里没有插件 jar，先跑 tools\build-plugin.ps1' }
    $PluginJar = $cand.FullName
}
if (-not (Test-Path $PluginJar)) { throw "找不到插件 jar: $PluginJar" }
if (-not (Test-Path $PaperJar)) { throw "找不到 Paper jar: $PaperJar" }
if (-not (Test-Path $JavaExe)) {
    $alt = Get-Command java -ErrorAction SilentlyContinue
    if (-not $alt) { throw "找不到 java: $JavaExe" }
    $JavaExe = $alt.Source
    Write-Host "改用 PATH 里的 java: $JavaExe" -ForegroundColor Yellow
}

# 服务端控制台输出的是系统 ANSI 编码（本机 GBK），不是 UTF-8，所以要按字节判断
# 注意：日志文件被服务端进程占着，必须用 FileShare.ReadWrite 打开才读得到
function Read-LogText([string]$path) {
    if (-not (Test-Path $path)) { return '' }
    $bytes = $null
    try {
        $fs = [System.IO.File]::Open($path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
        try {
            $ms = New-Object System.IO.MemoryStream
            $fs.CopyTo($ms)
            $bytes = $ms.ToArray()
            $ms.Dispose()
        }
        finally { $fs.Dispose() }
    }
    catch { return '' }
    if (-not $bytes -or $bytes.Length -eq 0) { return '' }
    try {
        $utf8 = New-Object System.Text.UTF8Encoding($false, $true)
        return $utf8.GetString($bytes)
    }
    catch {
        return [System.Text.Encoding]::GetEncoding(936).GetString($bytes)
    }
}

$test = Join-Path $env:TEMP ('mmap-plugin-test-' + (Get-Date -Format 'HHmmss'))
New-Item -ItemType Directory $test -Force | Out-Null
New-Item -ItemType Directory (Join-Path $test 'plugins') -Force | Out-Null

try {
    Write-Host "测试服目录: $test"
    Copy-Item $PaperJar (Join-Path $test 'Paper-1.20.1.jar') -Force
    Set-Content (Join-Path $test 'eula.txt') 'eula=true' -Encoding ASCII
    @"
online-mode=false
level-name=testworld
server-port=$Port
view-distance=4
simulation-distance=4
spawn-protection=0
max-players=5
motd=MMmap plugin smoke test
"@ | Set-Content (Join-Path $test 'server.properties') -Encoding ASCII

    $pluginName = Split-Path $PluginJar -Leaf
    Copy-Item $PluginJar (Join-Path $test "plugins\$pluginName") -Force
    Write-Host "装入插件: $pluginName"
    Write-Host ("   SHA256 {0}" -f (Get-FileHash $PluginJar -Algorithm SHA256).Hash)

    if ($WithMythicMobs) {
        $mm = Get-ChildItem 'E:\Minecraft\服务器\时旅服务端\plugins' -Filter 'MythicMobs-*.jar' -ErrorAction SilentlyContinue |
              Sort-Object Name -Descending | Select-Object -First 1
        if ($mm) { Copy-Item $mm.FullName (Join-Path $test 'plugins') -Force; Write-Host "同时装入: $($mm.Name)（测 MythicMobs 集成）" }
        else { Write-Host "没找到 MythicMobs jar，跳过" -ForegroundColor Yellow }
    }

    $log = Join-Path $test 'server.log'
    Write-Host "启动中（最多等 $TimeoutSeconds 秒）..."
    $proc = Start-Process -FilePath $JavaExe `
        -ArgumentList '-Xms512M', '-Xmx1500M', '-jar', 'Paper-1.20.1.jar', 'nogui' `
        -WorkingDirectory $test `
        -RedirectStandardOutput $log -RedirectStandardError (Join-Path $test 'server.err.log') -PassThru

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $done = $false
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
        $text = Read-LogText $log
        if ($text -match 'Done \(') { $done = $true; break }
        if ($proc.HasExited) { break }
    }
    Start-Sleep -Seconds 3
    if (-not $proc.HasExited) { $proc.Kill(); Start-Sleep -Seconds 4 }

    $text = Read-LogText $log
    $lines = $text -split "`r?`n"

    Write-Host ''
    Write-Host '=== 插件相关日志 ===' -ForegroundColor Cyan
    $lines | Where-Object { $_ -match 'MMmap' } | ForEach-Object { Write-Host $_ }

    $bad = $lines | Where-Object { $_ -match 'ERROR|Exception|Caused by|Could not load|Failed to load' }
    if ($bad) {
        Write-Host ''
        Write-Host '=== 发现错误 ===' -ForegroundColor Red
        $bad | Select-Object -First 30 | ForEach-Object { Write-Host $_ }
    }

    $enabled = ($lines | Where-Object { $_ -match '已成功启用' }).Count -gt 0
    $loaded  = ($lines | Where-Object { $_ -match 'Loading server plugin MMmap' }).Count -gt 0

    Write-Host ''
    if ($loaded -and $enabled -and -not $bad) {
        Write-Host '结果: 通过 ✔  插件正常加载并启用，没有异常' -ForegroundColor Green
        if (-not $done) { Write-Host '（提示：没等到 Done 就结束了，请确认日志）' -ForegroundColor Yellow }
        exit 0
    }
    else {
        Write-Host ('结果: 不通过 ✘  loaded={0} enabled={1} errors={2}' -f $loaded, $enabled, $bad.Count) -ForegroundColor Red
        Write-Host "完整日志: $log"
        exit 1
    }
}
finally {
    if (-not $KeepServer) {
        Remove-Item $test -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "已清理测试目录（要保留请加 -KeepServer）"
    }
    else {
        Write-Host "测试目录保留在: $test"
    }
}
