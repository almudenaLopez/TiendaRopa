# TiendaRopa

Aplicación de consola en Java para practicar JDBC con una tienda de ropa. Permite consultar el catálogo, crear y cancelar pedidos, controlar el stock y consultar información de ventas.

## Requisitos

- JDK 25.
- IntelliJ IDEA con soporte para proyectos Maven.
- H2, incluido como dependencia Maven, para ejecutar la aplicación localmente.
- MySQL instalado y en ejecución solo si se quiere probar el modo MySQL.

## Configuración


Las propiedades principales son:

- `database.engine`: `H2` o `MYSQL`.
- `connection.mode`: `DRIVER_MANAGER` o `POOL` para usar el pool HikariCP.
- `simulation.mode`: `NORMAL` o `FALLO_TRANSACCION` para provocar y comprobar la reversión de un pedido.
- `h2.url`, `h2.user` y `h2.password`: conexión local H2.
- `mysql.url`, `mysql.user` y `mysql.password`: conexión MySQL.

La configuración de ejemplo utiliza H2 y almacena la base en `data/unidad2.mv.db`. No hace falta instalar H2 por separado. Para MySQL hay que tener el servidor funcionando y revisar las credenciales de la configuración.

## Ejecución en IntelliJ IDEA

1. Abre el proyecto Maven y espera a que IntelliJ cargue las dependencias.
2. Abre `src/main/java/es/tiendaderopa/app/Main.java`.
3. Ejecuta el método `main` con el icono verde.
4. La aplicación prepara el esquema y carga datos de ejemplo cuando la base de datos está vacía.

## Opciones del menú

1. Buscar prendas por categoría, talla, color y disponibilidad.
2. Crear un pedido con uno o varios artículos; se comprueba el stock y se registra el pago simulado.
3. Cancelar un pedido confirmado; se anula el pago simulado y se devuelve el stock.
4. Reponer artículos y registrar el movimiento de entrada.
5. Consultar artículos cuyo stock está en el mínimo o por debajo.
6. Consultar unidades vendidas por producto; los pedidos cancelados no cuentan como ventas.
7. Recomendar artículos disponibles según la talla y el color preferidos de un cliente.
8. Consultar el historial de pedidos y sus líneas para un cliente.
0. Salir.

La creación y cancelación de pedidos y la reposición de stock usan transacciones JDBC para mantener sincronizados los pedidos, pagos, existencias y movimientos.
