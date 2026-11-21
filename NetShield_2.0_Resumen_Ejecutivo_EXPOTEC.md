# EXPOTEC — RESUMEN EJECUTIVO

| ![NetShield Logo](https://raw.githubusercontent.com/feathericons/feather/master/icons/shield.svg) | **NETSHIELD 2.0: SISTEMA AVANZADO DE ANÁLISIS, MONITOREO DE REDES Y DETECCIÓN DE ANOMALÍAS**<br>**Categoría:** Ciberseguridad, Redes de Computadoras y Programación Avanzada<br>**Integrantes:**<br>1.- Ramos Fernández Josué Miguel |
| :--- | :--- |
| ✉ | **Correo electrónico del representante:** Jramosf7@autonoma.edu.pe |
| ✆ | **Teléfono del representante:** 907877310 |

---

### Justificación del proyecto
En la actualidad, la seguridad de las infraestructuras de red representa uno de los desafíos más críticos tanto para empresas como para instituciones académicas. El aumento constante de escaneos no autorizados, intrusiones y ataques de denegación de servicio (DDoS) expone la vulnerabilidad de sistemas que no cuentan con supervisión constante. Aunque existen reconocidas herramientas profesionales en el mercado (como Nmap o Wireshark), la gran mayoría opera mediante consolas de comandos complejas, requiere la instalación forzosa de controladores nativos externos (como WinPcap o Npcap con permisos de administrador) o carece de una capa visual que correlacione en tiempo real el escaneo de puertos con la degradación del enlace físico.

Asimismo, muchas pequeñas organizaciones y centros educativos no disponen del presupuesto ni del personal técnico especializado para operar complejas plataformas SIEM (*Security Information and Event Management*). Por ello, este proyecto surge con el propósito de desarrollar **NetShield 2.0**, una solución integral de escritorio en Java 17 y JavaFX que combina el escaneo exhaustivo de puertos TCP (1 a 65,535), la inspección de conexiones activas en tiempo real, la detección heurística de anomalías mediante algoritmos estadísticos (Media Móvil Exponencial - EMA) y un laboratorio de resiliencia ante sobrecargas, garantizando un entorno autónomo, sin dependencias de librerías nativas y con una experiencia de usuario moderna y accesible.

---

### Objetivo del proyecto
Desarrollar una aplicación de escritorio multiplataforma utilizando el lenguaje de programación Java 17 y el framework JavaFX, denominada **NetShield 2.0 Pro**, capaz de auditar la seguridad de redes y evaluar su resiliencia operativa. El sistema integra un motor de escaneo concurrente de alta velocidad con discriminación de estados (`OPEN`, `CLOSED`, `FILTERED`) y extracción de banners (*fingerprinting*), monitoreo nativo de tráfico y conexiones activas con cálculo de velocidad de transferencia ($\text{KB/s}$), un módulo de detección de anomalías basado en la desviación de latencia ($RTT$) y el análisis de ráfagas, y un laboratorio de pruebas de carga controlada con temporizadores automáticos de seguridad. Adicionalmente, el proyecto busca consolidar la generación de reportes técnicos ejecutivos en formatos PDF, CSV y JSON para facilitar la remediación de vulnerabilidades.

---

### Impacto del proyecto en la sociedad

* **Ámbito Tecnológico y de Ciberseguridad:** Proporciona una herramienta de diagnóstico no intrusiva y 100% nativa que no requiere controladores externos. Permite a administradores de red identificar puertos expuestos, banners de versiones desactualizadas y evaluar la susceptibilidad de sus servidores ante sobrecargas, mitigando riesgos de intrusión y pérdida de información.
* **Ámbito Educativo:** Se constituye como una potente plataforma pedagógica para estudiantes y docentes de Ingeniería de Sistemas y Telecomunicaciones. Permite experimentar de forma visual conceptos fundamentales de redes como el saludo de tres vías TCP (*three-way handshake*), los paquetes `RST` y `DROP`, la fluctuación de fase (*jitter*) y el impacto de la concurrencia en la disponibilidad de los servicios.
* **Accesibilidad y Eficiencia:** Al estar desarrollado sobre la máquina virtual de Java (JVM) y optimizado mediante un modelo de ejecución no bloqueante con *batching* de eventos, NetShield 2.0 puede ejecutarse fluidamente en equipos de recursos modestos sin degradar el rendimiento del sistema operativo anfitrión.

---

### Beneficio comercial del proyecto
NetShield 2.0 posee un amplio potencial de transferencia tecnológica y comercial dentro de los sectores de software de seguridad (SecOps) y tecnología educativa (EdTech):

1. **Licenciamiento Educativo e Institucional:** Puede comercializarse e implementarse en universidades, institutos técnicos y laboratorios de formación en redes como una suite didáctica interactiva para cátedras de Ciberseguridad, Redes de Comunicación y Programación Concurrente.
2. **Servicio de Auditoría Rápida para PyMEs:** Las pequeñas y medianas empresas con frecuencia no pueden costear auditorías externas de alto valor. NetShield 2.0 permite a consultores de TI independientes realizar evaluaciones rápidas de vulnerabilidades (*vulnerability assessments*), entregando informes ejecutivos y planes de mitigación inmediatos en PDF.
3. **Modelo Open-Core / Soporte Especializado:** El núcleo de la herramienta puede distribuirse para uso comunitario, ofreciendo módulos empresariales de pago que incluyan monitoreo continuo 24/7 y pasarelas de exportación hacia bases de datos corporativas.

---

### Vinculación con los Objetivos de Desarrollo Sostenible (ODS)

* **ODS 9: Industria, Innovación e Infraestructura:**
  NetShield 2.0 fortalece la seguridad y resiliencia de las infraestructuras de tecnología de la información, protegiendo los canales digitales y promoviendo el desarrollo de software seguro e innovador a nivel local.
* **ODS 4: Educación de Calidad:**
  El proyecto democratiza el aprendizaje práctico de la ciberseguridad y la ingeniería de redes, transformando conceptos teóricos y abstractos en experiencias visuales interactivas que potencian las competencias técnicas de los futuros profesionales de ingeniería.

---

### Carácter innovador
A diferencia de los escáneres convencionales de consola o suites complejas que requieren controladores nativos (`.dll` o `.so`), NetShield 2.0 integra múltiples disciplinas de red en una sola arquitectura cohesiva:

* **Innovación Algorítmica y Detección Adaptativa (EMA):** Implementa un filtro dinámico de Media Móvil Exponencial (EMA) para modelar la línea base del enlace físico. En lugar de utilizar umbrales fijos arbitrarios, NetShield calcula el *jitter* en tiempo real ($\Delta = RTT_t - EMA_{t-1}$) y detecta picos de latencia anómalos provocados por saturación de ancho de banda o ataques de denegación de servicio.
* **Monitoreo Nativo sin Drivers de Terceros:** Recopila métricas de tráfico y conexiones TCP activas (`ESTABLISHED`, `LISTENING`, `TIME_WAIT`) inspeccionando directamente el subsistema del sistema operativo, eliminando la dependencia de WinPcap/Npcap y garantizando compatibilidad inmediata en cualquier ordenador.
* **Diseño y Ergonomía Visual (Cinemática JavaFX):** Sustituye los contenedores estáticos por una navegación animada con aceleración cúbica (*Spline Easing*) a 60 FPS con recorte de *viewport*, tarjetas KPI dinámicas y una barra de búsqueda instantánea capaz de filtrar y ordenar fluidamente hasta 65,535 puertos evaluados.
* **Laboratorio DDoS con Salvaguardas de Seguridad:** Permite simular estrés de red de manera estrictamente local y ética, incorporando temporizadores con cuenta regresiva automática, métricas de solicitudes exitosas vs. rechazadas, telemetría de memoria Heap de la JVM y botón de parada de emergencia.

---

### Referencias Bibliográficas (Normas APA 7.ª edición)

* Forouzan, B. A. (2018). *Transmisión de datos y redes de comunicaciones* (5.ª ed.). McGraw-Hill Education.
* Kurose, J. F., & Ross, K. W. (2021). *Computer networking: A top-down approach* (8.ª ed.). Pearson.
* Lyon, G. F. (2009). *Nmap network scanning: The official Nmap project guide to network discovery and vulnerability scanning*. Insecure.com LLC.
* Oracle Corporation. (2023). *Java Platform, Standard Edition & JavaFX Documentation (Release 17 / 21)*. Oracle Help Center. https://docs.oracle.com/en/java/
* Stallings, W. (2017). *Cryptography and network security: Principles and practice* (7.ª ed.). Pearson.
* Tanenbaum, A. S., & Wetherall, D. J. (2011). *Redes de computadoras* (5.ª ed.). Pearson Educación.
