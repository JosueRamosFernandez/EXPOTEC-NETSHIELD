# NetShield 2.0 Pro

Sistema Avanzado de Análisis, Monitoreo de Redes y Detección de Anomalías por Latencia desarrollado en **Java 17** y **JavaFX 21** (compatible con **Apache NetBeans** y **Maven**).

## Características Principales
- **Escaneo Concurrente de Puertos TCP (1 – 65,535):** Discriminación de estados (`OPEN`, `CLOSED`, `FILTERED`), *banner grabbing* (`BannerGrabber`), barra de búsqueda y filtros en tiempo real.
- **Detección Adaptativa de Anomalías (EMA):** Cálculo dinámico de línea base mediante Media Móvil Exponencial (`LatencyBaseline`) para detectar picos anómalos de *jitter* y latencia `RTT`.
- **Monitoreo de Tráfico en Vivo y Saturación:** Inspección nativa de sockets TCP (`ESTABLISHED`, `LISTENING`, `TIME_WAIT`), velocidad de descarga/subida (`KB/s`) y ranking de puertos con posible saturación sin requerir drivers externos.
- **Descubrimiento de Subred `/24`:** Mapeo ICMP concurrente de hosts activos y envío directo al escáner.
- **Laboratorio Local de Resiliencia (`127.0.0.1`):** Servidor de demostración (`8080–8082`) y banco controlado de carga con temporizador automático y parada de emergencia.
- **Reportes Multi-Formato:** Exportación de auditorías y planes de mitigación en **PDF** (Apache PDFBox 3), **CSV** y **JSON**.

## Ejecución
- **Apache NetBeans:** Abrir el proyecto `NetShield-UI` y presionar `F6` (configurado mediante `nbactions.xml`).
- **Windows:** Ejecutar `ejecutar_NetShield.bat`.
- **Maven CLI:** `mvn clean javafx:run`

## Equipo
- Ramos Fernández Josué Miguel
- Ccasani Ticuña Angel Daniel
- Tapia Pareja Jose Luis
