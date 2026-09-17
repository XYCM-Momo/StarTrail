#Requires -Version 5.1
<#
.SYNOPSIS
    MMmap 工作区快照备份。

.DESCRIPTION
    把整个 MMmap 工作区打包成带时间戳的 zip 放进 backups\ 目录。
    默认排除 backups\ 自身和 Gradle 构建产物（build\、.gradle\）——这些随时能重新生成，
    排除后一个快照大约 10MB 左右，可以放心多留几份。

.PARAMETER Tag
    快照标签，会写进文件名，方便日后辨认。例如 -Tag 整理前 / -Tag v1.9.6发布。

.PARAMETER Destination
    备份存放目录，默认 <工作区>\backups。
    建议再定期把这个目录复制到别的盘（例如 E:\Minecraft\_backups\MMmap）。

.PARAMETER List
    只列出已有快照，不创建新的。

.PARAMETER Prune
    按保留数量清理最旧的快照（和 -Keep 一起用）。

.PARAMETER Keep
    -Prune 时保留的最新快照数量，默认 20。

.PARAMETER IncludeBuild
    连 build\ 和 .gradle\ 一起打包（体积约 +25MB，一般用不到）。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Tag 整理前

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -List

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Prune -Keep 10
#>
[CmdletBinding()]
param(
    [string]$Tag = 'manual',
    [string]$Destination,
    [switch]$List,
    [switch]$Prune,
    [int]$Keep = 20,
    [switch]$IncludeBuild
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Root = Split-Path -Parent $PSScriptRoot
if (-not $Destination) { $Destination = Join-Path $Root 'backups' }
if (-not (Test-Path $Destination)) { New-Item -ItemType Directory -Path $Destination -Force | Out-Null }

function Get-Snapshots {
    Get-ChildItem -Path $Destination -Filter 'MMmap_*.zip' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
}

if ($List) {
    $items = Get-Snapshots
    Write-Host "备份目录: $Destination"
    if (-not $items) { Write-Host '（还没有任何快照）'; return }
    $items | ForEach-Object {
        '{0}  {1,8:N2} MB  {2}' -f $_.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'), ($_.Length / 1MB), $_.Name
    }
    return
}

if ($Prune) {
    $items = Get-Snapshots
    if ($items.Count -le $Keep) {
        Write-Host "共 $($items.Count) 份快照，未超过保留数 $Keep，无需清理。"
        return
    }
    $items | Select-Object -Skip $Keep | ForEach-Object {
        Write-Host "删除旧快照: $($_.Name)"
        Remove-Item $_.FullName -Force
    }
    return
}

# ---- 要排除的相对路径前缀（统一用反斜杠、大小写不敏感比较）----
$exclude = @('backups\')
if (-not $IncludeBuild) {
    $exclude += @('mod-forge\build\', 'mod-forge\.gradle\', 'plugin-paper\build\', 'plugin-paper\.gradle\')
}

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$safeTag = ($Tag -replace '[\\/:*?"<>|\s]+', '-').Trim('-')
if (-not $safeTag) { $safeTag = 'manual' }
$zipPath = Join-Path $Destination ("MMmap_{0}_{1}.zip" -f $stamp, $safeTag)

$files = Get-ChildItem -Path $Root -Recurse -File -Force | Where-Object {
    $rel = $_.FullName.Substring($Root.Length + 1)
    $skip = $false
    foreach ($p in $exclude) {
        if ($rel.StartsWith($p, [System.StringComparison]::OrdinalIgnoreCase)) { $skip = $true; break }
    }
    -not $skip
}

if (-not $files) { throw "工作区里没有找到要备份的文件: $Root" }

$info = @(
    "MMmap 工作区快照"
    "时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    "来源: $Root"
    "标签: $Tag"
    "文件数: $($files.Count)"
    "说明: 已排除 backups\ 与 Gradle 构建产物（build\、.gradle\），它们可以重新生成。"
) -join "`r`n"

$archive = [System.IO.Compression.ZipFile]::Open($zipPath, 'Create')
try {
    foreach ($f in $files) {
        $rel = $f.FullName.Substring($Root.Length + 1).Replace('\', '/')
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archive, $f.FullName, $rel, [System.IO.Compression.CompressionLevel]::Optimal)
    }
    $entry = $archive.CreateEntry('_备份说明.txt', [System.IO.Compression.CompressionLevel]::NoCompression)
    $w = New-Object System.IO.StreamWriter($entry.Open(), (New-Object System.Text.UTF8Encoding($false)))
    $w.Write($info)
    $w.Dispose()
}
finally {
    $archive.Dispose()
}

$size = (Get-Item $zipPath).Length / 1MB
Write-Host ("快照完成: {0}" -f $zipPath)
Write-Host ("包含 {0} 个文件, 压缩后 {1:N2} MB" -f $files.Count, $size)
Write-Host ("SHA256: {0}" -f (Get-FileHash $zipPath -Algorithm SHA256).Hash)
Write-Host ''
Write-Host '恢复方法: 把 zip 解压覆盖回工作区即可（或只取需要的文件）。'
