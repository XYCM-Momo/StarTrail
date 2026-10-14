# StarTrail waypoint editor local server
# Purpose: open the editor via http://127.0.0.1 so the browser persists
# file-access permissions and folder memory as a real origin (unlike file://,
# which forgets everything after a browser restart).
# Usage: double-click 启动编辑器.cmd in this folder, or run this script directly.
# Close this window to stop the server.
$ErrorActionPreference = 'Stop'
$Port = 3081
$root = $PSScriptRoot

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://127.0.0.1:$Port/")
$listener.Start()
Write-Host ""
Write-Host "  StarTrail waypoint editor server started: http://127.0.0.1:$Port/"
Write-Host "  Close this window to stop the server."
Write-Host ""

$mime = @{
  '.html' = 'text/html; charset=utf-8'
  '.htm'  = 'text/html; charset=utf-8'
  '.js'   = 'text/javascript; charset=utf-8'
  '.mjs'  = 'text/javascript; charset=utf-8'
  '.json' = 'application/json; charset=utf-8'
  '.css'  = 'text/css; charset=utf-8'
  '.png'  = 'image/png'
  '.jpg'  = 'image/jpeg'
  '.jpeg' = 'image/jpeg'
  '.webp' = 'image/webp'
  '.gif'  = 'image/gif'
  '.svg'  = 'image/svg+xml'
  '.ico'  = 'image/x-icon'
  '.mmap' = 'application/octet-stream'
'.startrail' = 'application/octet-stream'
  '.yml'  = 'text/plain; charset=utf-8'
  '.yaml' = 'text/plain; charset=utf-8'
  '.md'   = 'text/plain; charset=utf-8'
}

while ($listener.IsListening) {
  $ctx = $null
  try {
    $ctx = $listener.GetContext()
    $path = [System.Uri]::UnescapeDataString($ctx.Request.Url.AbsolutePath)
    $rel  = $path.TrimStart('/')
    $file = Join-Path $root $rel
    if (Test-Path $file -PathType Container) { $file = Join-Path $file 'index.html' }
    if (Test-Path $file -PathType Leaf) {
      $bytes = [System.IO.File]::ReadAllBytes($file)
      $ext   = [System.IO.Path]::GetExtension($file).ToLower()
      $ct    = if ($mime.ContainsKey($ext)) { $mime[$ext] } else { 'application/octet-stream' }
      $ctx.Response.StatusCode = 200
      $ctx.Response.ContentType = $ct
      $ctx.Response.ContentLength64 = $bytes.Length
      $ctx.Response.OutputStream.Write($bytes, 0, $bytes.Length)
    } else {
      $ctx.Response.StatusCode = 404
    }
  } catch {
    try { if ($ctx) { $ctx.Response.StatusCode = 500 } } catch { }
  } finally {
    try { if ($ctx) { $ctx.Response.Close() } } catch { }
  }
}
