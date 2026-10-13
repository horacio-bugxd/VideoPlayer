# Video Player (Android 16)

Kotlin + Jetpack Compose + Media3 (ExoPlayer). compileSdk/targetSdk 36, minSdk 26.

Funciones: videos locales (Photo Picker, sin permisos), enlaces https, pantalla completa
al girar el teléfono, velocidad 0.5x–2x, subtítulos, Imagen en imagen (PiP) y "Abrir con".

## Cómo obtener el APK sin Android Studio

1. Crea una cuenta gratuita en github.com y un repositorio nuevo.
2. Sube TODO el contenido de esta carpeta (descomprimida), incluida la carpeta oculta `.github`.
3. Abre la pestaña **Actions** → "Build APK". Se ejecuta solo al subir los archivos (~5 min).
4. Entra a la ejecución terminada y descarga **VideoPlayer-APK** (sección Artifacts).
5. Descomprime, copia `app-debug.apk` al teléfono e instálalo
   (permite "instalar apps desconocidas" cuando Android lo pida).
