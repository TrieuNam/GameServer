@echo off
REM =============================================================================
REM Start Admin Service Database (Windows Batch Wrapper)
REM =============================================================================
REM Calls the PowerShell script to start admin DB

powershell -ExecutionPolicy Bypass -File "%~dp0start-admin-db.ps1"
pause
