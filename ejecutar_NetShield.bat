@echo off
title NetShield 2.0 Pro - Analisis y Monitoreo de Redes
cd /d "%~dp0"

echo ======================================================================
echo       NETSHIELD 2.0 PRO - SISTEMA DE MONITOREO Y SEGURIDAD DE RED
echo ======================================================================
echo Configurando entorno Java 17+...

if exist "C:\Program Files\Java\jdk-24" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    set "PATH=C:\Program Files\Java\jdk-24\bin;C:\apache-maven-3.9.11\bin;%PATH%"
    echo [OK] JDK 24 detectado y asignado como JAVA_HOME.
) else if exist "C:\Program Files\Apache NetBeans\jdk" (
    set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
    set "PATH=C:\Program Files\Apache NetBeans\jdk\bin;C:\apache-maven-3.9.11\bin;%PATH%"
    echo [OK] JDK de NetBeans detectado y asignado como JAVA_HOME.
)

echo Lanzando la aplicacion grafica NetShield 2.0 Pro...
echo (Presione Ctrl+C en esta consola si desea detener la app)
echo ======================================================================

mvn javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Ocurrio un error al ejecutar. Revise los mensajes superiores.
    pause
)
