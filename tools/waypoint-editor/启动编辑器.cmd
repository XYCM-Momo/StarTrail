@echo off
chcp 65001 >nul
cd /d "%~dp0"
start "StarTrail Editor Server" powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0_serve.ps1"
timeout /t 2 /nobreak >nul
start "" "http://127.0.0.1:3081/StarTrail%E7%82%B9%E4%BD%8D%E7%BC%96%E8%BE%91%E5%99%A8.html"
