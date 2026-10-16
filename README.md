# Full Hipertrofia — APK con GitHub

Este proyecto envuelve tu `index.html` en una app Android real. Incluye el puente nativo que la app ya espera (`AndroidFH`): avisos del teléfono, contador de pasos, comandos por voz, cámara, micrófono y selección de fotos.

## Cómo obtener el APK (sin instalar nada en tu PC)

1. Crea un repositorio nuevo en GitHub (déjalo **privado**; el proyecto incluye una llave de firma de uso personal).
2. Sube TODO el contenido de esta carpeta. Lo más seguro es con git:
   ```
   git init
   git add .
   git commit -m "Full Hipertrofia"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```
   Si lo subes arrastrando archivos en la web, GitHub puede omitir la carpeta oculta `.github`. En ese caso usa **Add file → Create new file**, escribe el nombre `.github/workflows/build-apk.yml` y pega el contenido de ese archivo.
3. Entra a la pestaña **Actions**. El flujo "Build APK" arranca solo con cada subida (también puedes usar **Run workflow**).
4. Cuando termine (5–8 minutos), abre la ejecución y descarga **FullHipertrofia-APK** (un zip con `FullHipertrofia.apk`).
5. Pasa el APK al teléfono, ábrelo y permite instalar de "orígenes desconocidos".

**APK en Releases:** si subes una etiqueta, por ejemplo `git tag v1.0 && git push origin v1.0`, el APK queda adjunto en la sección Releases del repositorio.

## Cómo actualizar la app
Reemplaza `app/src/main/assets/index.html` por la versión nueva y sube el cambio. Cada compilación usa la misma llave (`keystore/fh.jks`), así que el APK nuevo se instala encima del anterior sin perder datos. Si quieres, sube también `versionCode` en `app/build.gradle.kts`.

## Qué hace el contenedor nativo
- **Avisos:** al activarlos en la app, programa los tres recordatorios (ejercitar cada 4 h, comer cada 3 h, creatina cada 12 h). No avisa entre las 22:00 y las 07:00 y se reprograma al reiniciar el teléfono. En Android 13 o superior pide permiso de notificaciones.
- **Pasos:** usa el sensor de pasos del teléfono (pide el permiso "Actividad física").
- **Voz:** usa el reconocimiento de voz de Android, en español.
- **Cámara y micrófono:** piden permiso la primera vez. Para cargar fotos ofrece la cámara y la galería.
- **Pantalla:** se mantiene encendida mientras la app está abierta.

## Importante
- Los datos de la app instalada (kilos, reps, fotos) viven en el almacenamiento de la propia app. **No se copian** desde la versión web ni desde el navegador: empiezas con el registro vacío.
- Los videos de YouTube se cargan dentro de la app; si alguno no reproduce, usa el enlace "Abrir en YouTube".
- La compilación usa Gradle 8.9, Android Gradle Plugin 8.5.2, Kotlin 1.9.24 y JDK 17.
- El APK es de depuración firmado con la llave del proyecto: sirve para instalar en tu teléfono, pero no para subir a Google Play.
