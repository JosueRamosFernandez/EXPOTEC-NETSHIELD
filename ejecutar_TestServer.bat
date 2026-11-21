@echo off
title NetShield Testbed - Servidor de Pruebas
cd /d "%~dp0"

echo ======================================================================
echo           NETSHIELD TESTBED - SERVIDOR DE PRUEBAS LOCAL
echo ======================================================================
echo Configurando entorno Java 17+...

if exist "C:\Program Files\Java\jdk-24" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    set "PATH=C:\Program Files\Java\jdk-24\bin;C:\apache-maven-3.9.11\bin;%PATH%"
) else if exist "C:\Program Files\Apache NetBeans\jdk" (
    set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
    set "PATH=C:\Program Files\Apache NetBeans\jdk\bin;C:\apache-maven-3.9.11\bin;%PATH%"
)

echo Iniciando TestServer en puertos 8080, 8081 y 8082...
mvn compile exec:java -Dexec.mainClass="com.netshield.testbed.TestServer"

pause
