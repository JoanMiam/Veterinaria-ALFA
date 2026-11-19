# ALFA-INVENTORY — Inventario de la Veterinaria ALFA

Software de escritorio para llevar el inventario de medicamentos de la veterinaria ALFA
de Cancún, Quintana Roo: existencias por lote, caducidades, ventas, apartados y reportes.

## Requisitos

- Java 21 o superior
- Maven 3.9+

## Compilar, probar y ejecutar

```bash
mvn verify                      # compila, corre las pruebas (headless) y genera el jar
java -jar target/Refactorizacion-ALFA-1.0-SNAPSHOT.jar
```

- Las pruebas corren sin interfaz gráfica por defecto. Para incluir las de GUI:
  `mvn test -DargLine= -Dtest=FormularioConfirmableDialogTest`.
- En Java 24+ se puede agregar `--enable-native-access=ALL-UNNAMED` para evitar el aviso
  de SQLite al cargar su librería nativa.
- GitHub Actions ejecuta `mvn verify` en cada PR y en cada push a `main`/`develop`.

## Dónde quedan los datos

Todo se guarda junto al `.jar` / `.exe` (no en la carpeta desde donde se lanza):

| Archivo | Contenido |
|---|---|
| `db/baseDeDatosInventario.db` | Base de datos SQLite. **Respáldela** copiando este archivo con la app cerrada. |
| `config.properties` | Preferencia de tema (`theme=light` / `dark`). |
| `logs/alfa-N.log` | Registro de eventos y errores (5 archivos rotativos de 1 MB). |

Para usar otra carpeta: `java -Dalfa.home=/ruta -jar ...`.
Si existe una BD en `./db` de versiones anteriores, se **copia** a la nueva ubicación la
primera vez (el original no se toca).

## Arquitectura

```
InventarioApp            punto de entrada: log, tema, manejador global de errores
controller/
  InventarioController   capa de servicio para las vistas; avisa cambios (alCambiarInventario)
model/
  Producto, Venta, ConfiguracionVeterinaria   records del dominio
  InventarioDAO          acceso a SQLite; transacciones; sin Swing
  EsquemaBD              migraciones con PRAGMA user_version
  ReglasCaducidad        regla única de caducidad
  ReportesHtml           HTML de los reportes
  OperacionRechazadaException   regla de negocio no cumplida (mensaje para el usuario)
  ErrorDatosException           falla inesperada de la base de datos
view/
  *View                  ventanas y formularios Swing (FlatLaf)
  Estilos, RendererTabla, Dialogos, Formularios, Fechas, Tablas, FiltrosTabla, Tema
util/
  RutasApp, Registro, PreferenciasTema
```

Reglas de negocio principales:

- **Caducidad** `yyyy-MM`: el producto expira el primer día del mes siguiente. Desde 30 días
  antes se marca como "próximo a caducar". No se venden ni apartan productos caducados.
- **Lotes (FEFO)**: al buscar por nombre se usa el lote vendible que caduca primero.
- **Eliminar un producto** lo oculta (borrado lógico); su historial de ventas se conserva.
- **Eliminar una venta** regresa la cantidad al inventario.
- Las operaciones que tocan stock y ventas son transaccionales.

## Cambiar el esquema de la base de datos

1. En `model/EsquemaBD`, agregue un método `vN...` y llámelo desde `actualizar()` con
   `if (version < N)`.
2. Incremente `VERSION_ACTUAL`.
3. Nunca modifique un paso ya publicado: las BDs instaladas ya lo aplicaron.
4. Agregue una prueba en `EsquemaBDTest` que parta de la versión anterior.

## Convenciones

- Ramas `feature/NNN-...`, `fix/NNN-...`, `refactor/NNN-...`; PR hacia `develop`.
- Cada cambio se marca en el código con su identificador (`MR-00X`, `FIX-009`, `MR-010`)
  y sus pruebas siguen la nomenclatura `PRU-NNN-NN` / `TC-NN`.
