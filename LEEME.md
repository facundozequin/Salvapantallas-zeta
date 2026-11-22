# Zeta: fondo animado + salvapantallas (OnePlus 11)

App Android con tres partes:

- **Fondo animado:** el anillo "Somos Zeta · Digital Advice" gira y la Z está hecha de partículas que se dispersan al tocar la pantalla. Corre a 30 fps en reposo y a 60 fps cuando lo tocás, para ahorrar batería.
- **Salvapantallas:** se muestra mientras el teléfono carga. Las partículas forman el logo "SOMOS ZETA" y debajo aparecen la hora, la fecha y el porcentaje de batería. Todo se mueve muy despacio para que no se marque la pantalla AMOLED.
- **App "Zeta":** vista previa y botones para activar las dos cosas.

## Cómo sacar el APK

### Opción A: GitHub (sin instalar nada)
1. Creá un repositorio nuevo en GitHub y subí todo el contenido de esta carpeta, incluida la carpeta oculta `.github`.
2. Andá a la pestaña **Actions**. La compilación arranca sola y tarda unos 3–5 minutos.
3. Cuando termine con el tilde verde, entrá a la ejecución y bajá **zeta-apk** en "Artifacts". Es un .zip que trae `app-debug.apk`.

### Opción B: Android Studio (en la PC)
1. Abrí Android Studio, elegí **Open** y seleccioná esta carpeta.
2. Esperá a que termine el "Gradle sync". La primera vez descarga cosas.
3. Menú **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
   También podés conectar el teléfono por USB, con la depuración USB activada, y apretar ▶ Run.

## Instalar y activar en el OnePlus 11
1. Pasá el APK al teléfono y abrilo. Android te va a pedir permiso para "instalar apps desconocidas": aceptá para esa app (Archivos, Chrome, etc.).
2. Abrí la app **Zeta**:
   - **Poner como fondo de pantalla:** te muestra la vista previa del sistema. Elegí "Pantalla de inicio" o "Inicio y bloqueo".
   - **Configurar salvapantallas:** elegí "Zeta" y en "Cuándo iniciar" marcá **Mientras se carga**.

> En algunas versiones de OxygenOS el menú de salvapantallas está oculto en Ajustes. El botón de la app lo abre igual. Si tu versión no lo trae, el fondo animado funciona igual.

## Personalizar
- Imágenes: `app/src/main/res/drawable-nodpi/` (`ring.png` = logo circular, `word.png` = logo horizontal).
- Densidad de partículas, tamaños y velocidades: constantes al principio de `ZetaRenderer.java` (`Z_GRID`, `W_GRID`).

## App AG
En la carpeta `ag/` está la misma app con el logo AG, en blanco, negro y grises. Se instala aparte como **AG**, así que puede convivir con Zeta. La compilación genera los dos APK y los publica en Releases: `Zeta.apk` y `AG.apk`.
