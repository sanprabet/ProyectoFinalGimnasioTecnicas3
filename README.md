# Gestión de negocio y gimnasio

Aplicación de escritorio para un negocio pequeño, escrita en Java 21 con Swing. La ventana principal es un `JFrame` y los formularios se abren en diálogos. Se compila con el JDK y usa archivos locales, sin base de datos.

## Negocio

- Agregar y editar productos y servicios con un código único y un precio en pesos colombianos (COP).
- Consultar y ajustar las existencias de cada producto. Las ventas descuentan unidades y se rechazan cuando el inventario es insuficiente.
- Registrar ventas por artículo, cantidad y medio de pago. El historial conserva el precio aplicado a cada venta.
- Registrar gastos por fecha y concepto. El resumen mensual muestra ventas, gastos y el saldo calculado como la diferencia entre ambos.

## Gimnasio

- Agregar, editar, buscar y eliminar socios. Cada documento identifica a un único socio.
- Gestionar planes mensuales, trimestrales y anuales. La membresía es válida hasta el día de vencimiento inclusive.
- Registrar ingresos de socios con membresía vigente y ordenar el historial por fecha, nombre o documento.
- Consultar el total de socios, las membresías vigentes y los ingresos del día.

## Ejecutar con Docker

Desde la raíz del repositorio, con Docker y Docker Compose disponibles, ejecuta el siguiente comando.

```sh
docker compose up --build -d
```

Abre [la aplicación](http://localhost:6080/vnc.html?autoconnect=true&resize=scale) en el navegador. noVNC muestra la ventana Swing que se ejecuta en el contenedor. Compose publica el puerto únicamente en `127.0.0.1` y configura la zona horaria `America/Bogota`. Si el puerto `6080` está ocupado, puedes cambiarlo con `GIMNASIO_PUERTO=6081 docker compose up -d` y abrir el enlace con el puerto elegido.

Para detener la aplicación, usa `docker compose down`. El volumen `gimnasio-datos` conserva `gimnasio.properties` y `negocio.properties` entre reinicios. El historial de ingresos del gimnasio se conserva al eliminar un socio.

## Ejecutar con Java en macOS o Linux

Instala el JDK 21 y ejecuta estos comandos desde la raíz del proyecto.

```sh
sh build.sh
java -jar build/gimnasio.jar
```

La aplicación crea la carpeta `data/` al iniciar. La variable de entorno `GYM_DATA_DIR` permite seleccionar otra carpeta de datos.
