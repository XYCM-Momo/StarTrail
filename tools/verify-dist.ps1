#Requires -Version 5.1
<#
.SYNOPSIS
    校验 / 刷新 dist\ 成品目录的 SHA256 清单。

.DESCRIPTION
    不带参数：列出 dist\ 下所有 jar 的 SHA256 与大小。
    -Write  ：重新生成 dist\SHA256SUMS.txt（发布后调用）。
    -Check  ：把当前文件与 SHA256SUMS.txt 里的记录比对，有不一致就报错退出（用于确认
              “手上这个 jar 是不是当初发布的那一个”）。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\verify-dist.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\verify-dist.ps1 -Check
#>
[CmdletBinding()]
param(
    [switch]$Write,
    [switch]$Check
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Dist = Join-Path $Root 'dist'
$SumsFile = Join-Path $Dist 'SHA256SUMS.txt'

$files = Get-ChildItem -Path $Dist -Recurse -File |
    Where-Object { $_.Extension -in @('.jar', '.zip') } |
    Sort-Object FullName

if (-not $files) { throw "dist\ 里没有成品文件: $Dist" }

$lines = foreach ($f in $files) {
    $rel = $f.FullName.Substring($Dist.Length + 1).Replace('\', '/')
    $hash = (Get-FileHash $f.FullName -Algorithm SHA256).Hash
    [pscustomobject]@{ Rel = $rel; Hash = $hash; Size = $f.Length; Time = $f.LastWriteTime }
}

if ($Check) {
    if (-not (Test-Path $SumsFile)) { throw "缺少 $SumsFile，先跑一次 -Write" }
    $recorded = @{}
    foreach ($line in Get-Content $SumsFile) {
        if ($line -match '^([0-9A-Fa-f]{64})\s+\*?(.+)$') { $recorded[$Matches[2].Trim()] = $Matches[1].ToUpper() }
    }
    $bad = 0
    foreach ($l in $lines) {
        if (-not $recorded.ContainsKey($l.Rel)) { Write-Host "新增（未记录）: $($l.Rel)" -ForegroundColor Yellow; $bad++; continue }
        if ($recorded[$l.Rel] -ne $l.Hash) { Write-Host "内容不一致: $($l.Rel)" -ForegroundColor Red; $bad++ }
        else { Write-Host "OK  $($l.Rel)" -ForegroundColor Green }
    }
    foreach ($k in $recorded.Keys) {
        if (-not ($lines.Rel -contains $k)) { Write-Host "记录里有、但文件已不在: $k" -ForegroundColor Yellow }
    }
    if ($bad -gt 0) { throw "校验不通过：$bad 个文件有问题" }
    Write-Host '全部一致。' -ForegroundColor Green
    return
}

$lines | ForEach-Object { '{0}  {1,9:N0}  {2}  {3}' -f $_.Hash, $_.Size, $_.Time.ToString('yyyy-MM-dd HH:mm'), $_.Rel }

if ($Write) {
    $header = @(
        "# MMmap 成品校验清单 (dist\)",
        "# 生成时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
        "# 校验: tools\verify-dist.ps1 -Check",
        ''
    )
    $body = $lines | ForEach-Object { '{0}  {1}' -f $_.Hash, $_.Rel }
    $text = ($header + $body) -join "`r`n"
    [System.IO.File]::WriteAllText($SumsFile, $text + "`r`n", (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ''
    Write-Host "已写入 $SumsFile" -ForegroundColor Green
}
