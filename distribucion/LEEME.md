# APK para repartir a los cadetes

`cadete-app-debug.apk` es la APK que se pasa a los celulares de los cadetes (por Bluetooth, sin Play
Store). Se actualiza **a mano**, solo cuando hay una versión para repartir:

```bash
./gradlew assembleDebug -Dorg.gradle.java.home=<ruta a un JDK 17>
cp app/build/outputs/apk/debug/app-debug.apk distribucion/cadete-app-debug.apk
git add distribucion/cadete-app-debug.apk
```

Antes (hasta 2026-09-29) se versionaba directamente `app/build/outputs/apk/debug/app-debug.apk`, y cada
compilación dejaba el repo modificado. Ahora `app/build/` no va al repo.

Ojo: la que está acá puede ser **más vieja que el código** (la última copiada es de antes del control
"en el lugar" y de los avisos de la calle, `versionCode` 1). Antes de repartir, compilar y copiar.
