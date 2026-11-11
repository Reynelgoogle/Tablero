# Mesa · Tableros kanban 100% offline para Android

Mesa es una aplicación nativa para Android de tableros kanban que funciona **100% offline, sin cuentas, sin servidores y con privacidad total**. Todos los tableros se almacenan de manera local y persistente en el dispositivo.

## Características principales

- **Múltiples tableros**: Creación, conmutación rápida, personalización de colores, duplicación y eliminación.
- **Columnas y listas**: Organización Kanban por columnas con reordenación, ordenación alfabética o por vencimiento, duplicación y vaciado.
- **Tarjetas completas**:
  - Títulos y descripciones con formato multilínea.
  - Etiquetas a color personalizables por cada tablero.
  - Fechas de vencimiento con indicadores inteligentes de estado (vencida, hoy, próximas).
  - Listas de tareas (checklists) con indicador de progreso.
  - Movimiento rápido entre columnas y reordenación vertical.
  - Copiado de contenido al portapapeles y duplicación.
- **Búsqueda instantánea y filtros**: Búsqueda en tiempo real por título, descripción y tareas, más filtros dinámicos por vencimiento y etiquetas.
- **Historial Deshacer/Rehacer (Undo / Redo)**: Pila de historial para revertir o recuperar acciones en cualquier momento.
- **Temas Claro y Oscuro**: Paleta visual cálida inspirada en la versión original con modo oscuro nativo.
- **Respaldos JSON**: Exportación y restauración de respaldos completos en formato JSON compatibles con la versión web original.

## Arquitectura y Tecnologías

- **Lenguaje**: Kotlin
- **UI Framework**: Jetpack Compose con Material Design 3
- **Arquitectura**: MVVM (Model-View-ViewModel) con Kotlin Coroutines y StateFlow
- **Persistencia**: Almacenamiento local seguro en JSON (Gson)
- **SDK**: Android SDK 36 (minSdk 26)

## Estructura del proyecto

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── MainActivity.kt
│   ├── model/
│   │   └── MesaModels.kt
│   ├── data/
│   │   └── MesaRepository.kt
│   └── ui/
│       ├── MesaApp.kt
│       ├── MesaViewModel.kt
│       ├── theme/
│       │   ├── Color.kt
│       │   └── Theme.kt
│       └── components/
│           ├── MesaTopBar.kt
│           ├── BoardHeader.kt
│           ├── ListColumn.kt
│           ├── CardItem.kt
│           ├── CardDetailDialog.kt
│           └── BoardDialogs.kt
└── res/
    ├── drawable/
    ├── mipmap-*/
    └── values/
        └── strings.xml
```
