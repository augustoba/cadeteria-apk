# cadete-app — App Android de cadetes

App nativa Android (Kotlin + Jetpack Compose) para los cadetes de reparto. Es el tercer
componente del sistema, junto a `cadeteria/` (backend) y `admin-front/` (panel web) —
ver el [`README.md`](../README.md) de la raíz para el panorama completo, y
[`spec-app-cadetes.md`](../spec-app-cadetes.md) / [`diseno-tecnico.md`](../diseno-tecnico.md)
para la spec funcional y el diseño técnico completos.

**Estado: código completo según la spec v1, sin compilar/probar en un dispositivo real**
(este entorno no tiene Android Studio/SDK/emulador). Antes de repartirla a los cadetes,
alguien con Android Studio tiene que: abrir el proyecto, dejar que baje las dependencias,
generar el wrapper de Gradle si hace falta, compilar, y probarla en al menos un
dispositivo real (idealmente uno viejo tipo Galaxy J7, que es el piso de compatibilidad
del spec). Ver "Primer build" y "Qué falta probar" más abajo.

## Qué hace

Según `spec-app-cadetes.md` sección 3 y 5:

- **Login** contra el mismo backend que el panel admin (`POST /api/auth/login/cadete`).
- Botón para marcarse **Libre/Desconectado** — mientras está Libre, un foreground
  service manda la ubicación cada `frecuencia_ubicacion_seg` (configurable desde el
  panel admin, se lee de `GET /api/cadetes/me/configuracion`).
- Recibe el **viaje asignado** (no ve la lista completa de pedidos, solo el suyo) por
  WebSocket/STOMP si la app está abierta, o push (FCM) si está en background/cerrada —
  puede **aceptar o rechazar**.
- Al aceptar, ve el **destino en un mapa** (OpenStreetMap vía osmdroid) con la **ruta
  sugerida** (OpenRouteService, si el backend tiene la API key configurada).
- Botón **"Retirado"**: lo toca cuando pasa por lo del cliente a buscar el pedido, para
  que el panel admin sepa que ya lo tiene en camino — la foto del retiro es opcional.
- Al **finalizar**, ahora sí son **obligatorios los dos datos**: nombre y apellido de
  quien recibió, y una foto de la entrega o del frente del domicilio (antes alcanzaba
  con uno de los dos — se endureció a pedido). Las fotos suben directo a Cloudinary,
  igual que hace `admin-front` con las de cadete/vehículo — el backend nunca ve el
  archivo, solo la URL resultante.
- Tanto "Retirado" como "Finalizar" mandan también la **última ubicación conocida del
  GPS** (un solo tiro vía `FusedLocationProviderClient.lastLocation`, no requiere que el
  foreground service de tracking esté activo) — es opcional, si no hay fix disponible el
  campo va `null` y no bloquea el flujo. El admin la usa desde el panel para verificar
  en un mapa que el retiro/entrega coincide con la dirección real declarada por el
  cliente (ver `location/UbicacionActual.kt`).
- **Chat interno** con el admin, en tiempo real (mismo hub STOMP que la ubicación) con
  respaldo por push cuando la app no está abierta.
- Sección **"Asignados y en curso"**: lista todos los viajes activos del cadete (no
  asume uno solo — soporta `maxViajesSimultaneos > 1`), cada uno abre su propia pantalla
  de detalle (`GET /pedidos/me/{id}`, no solo `/activo`).
- Sección **"Finalizados"**: historial de viajes cerrados + resumen (cantidad, monto
  total cobrado, cuántos rechazó, cuántos no aceptó a tiempo y se le reasignaron a otro).
  Pagina de a 20 con un botón "Mostrar más" (paginado simple del lado del cliente, no
  hay `?page=` contra el backend — alcanza mientras el historial no sea gigante).
- **Perfil**: la mayoría de los datos (foto, vehículo, topes) los carga el admin desde
  el panel y son de solo lectura acá, pero el cadete puede **cambiar su contraseña, su
  teléfono, y cargar su CBU/alias** (para que el cliente le transfiera — el admin lo ve
  de solo lectura en el panel, y el cliente lo ve en la página pública de seguimiento).
- **Avisos**: mensajes generales del admin ("cerramos temprano") y recordatorios
  automáticos si un viaje se demora (sin retirar / sin finalizar, umbrales configurables
  desde el panel) — llegan como notificación con vibración y sonido fuerte a propósito,
  no solo un banner en pantalla.
- Pantalla de **configuración del servidor**: no hay Play Store ni distribución
  centralizada (la APK se instala a mano por Bluetooth, spec 2), así que la URL del
  backend es editable desde la app, no está fija en el build.

## Stack y decisiones

- **Kotlin + Jetpack Compose (Material 3)**, `minSdk 24` / `compileSdk 34` — igual que
  define la spec (sección 2).
- **Sin Hilt/Dagger ni Room**: la app es chica (un solo usuario logueado, una pantalla
  activa a la vez) — un service locator manual (`CadeteApp.kt`) y DataStore Preferences
  para la sesión alcanzan sin sumar un annotation processor más al build.
- **Retrofit + OkHttp + Gson** para REST, contra los mismos endpoints reales del
  backend (`ApiService.kt` está armado leyendo los `@Controller` de Java directamente,
  no solo el diseño en papel — ver "Contrato con el backend" abajo).
- **Cliente STOMP escrito a mano** (`realtime/StompClient.kt`) sobre el WebSocket de
  OkHttp, en vez de una librería de terceros — el protocolo que hace falta (CONNECT/
  SUBSCRIBE/MESSAGE/heart-beat, sin SEND) es chico y así no se ata el proyecto a una
  librería STOMP para Android que puede estar sin mantener.
- **osmdroid** para el mapa (mismo proveedor — OpenStreetMap — que usa `admin-front`
  con Leaflet, coherente con el resto del sistema y sin costo).
- **Coil** para las fotos (perfil, vehículo) que vienen de Cloudinary.
- **Firebase Cloud Messaging** para push, con **degradación intencional**: si no se
  configura `google-services.json`, la app compila y funciona igual (el WebSocket es
  el canal principal; el push es el respaldo para cuando la app está cerrada — ver
  diseno-tecnico.md sección 4). Ver "Push (Firebase)" abajo.

## Estructura

```
app/src/main/java/com/cadeteria/cadete/
├── CadeteApp.kt              # Application — arma todas las dependencias (service locator)
├── MainActivity.kt
├── data/
│   ├── remote/
│   │   ├── ApiService.kt     # Retrofit — un método por endpoint real del backend
│   │   ├── RetrofitProvider.kt
│   │   ├── AuthInterceptor.kt
│   │   └── dto/*.kt          # espejan los records DTO del backend (Java) uno a uno
│   ├── local/SessionManager.kt   # DataStore: URL del backend, token, cadeteId
│   └── repository/*.kt       # Auth/Cadete/Pedido/Chat + CloudinaryUploader
├── realtime/
│   ├── StompClient.kt        # cliente STOMP hecho a mano (ver arriba)
│   └── RealtimeManager.kt    # conecta, se suscribe a los topics del cadete, reconecta
├── push/
│   ├── CadeteFirebaseMessagingService.kt
│   └── NotificationHelper.kt
├── location/
│   ├── LocationTrackingService.kt   # foreground service, manda ubicación periódica
│   └── LocationServiceController.kt
└── ui/
    ├── theme/                 # Material 3, mismo azul de marca que admin-front
    ├── navigation/            # NavHost + rutas
    ├── servidor/ login/ home/ viaje/ chat/ perfil/   # una pantalla por carpeta, con su ViewModel
    └── common/                # composables y factory de ViewModel compartidos
```

## Contrato con el backend

`ApiService.kt` fue armado leyendo directamente `CadeteController.java`,
`PedidoCadeteController.java`, `PedidoAdminController.java`, `ChatController.java` y
sus DTOs — no solo `diseno-tecnico.md` (que en algún punto queda un paso atrás del
código real). Un cambio a tener en cuenta si tocás el backend:

**Se agregó un endpoint nuevo** que no estaba en el diseño original, porque hacía
falta y no existía nada parecido:

- `GET /api/cadetes/me/configuracion` (rol CADETE) → `{ frecuenciaUbicacionSeg,
  cloudinaryCloudName, cloudinaryUploadPreset }`. Antes, `/api/admin/configuracion`
  era la única forma de leer esos valores y es `hasRole("ADMIN")` — el cadete no podía
  leer ni la frecuencia de ubicación ni las credenciales de Cloudinary. Ver
  `ConfiguracionDtos.CadeteConfigResponse` y `CadeteController.configuracion()` en el
  backend.

**Más endpoints nuevos**, agregados junto con esta app (antes solo existía `/activo`,
singular, y no había forma de cambiar contraseña/teléfono/cuenta desde afuera del panel):

- `GET /api/pedidos/me/activos` — todos los viajes PENDIENTE/EN_CURSO del cadete (plural).
- `GET /api/pedidos/me/historial` — viajes finalizados + resumen (rechazados, no-aceptados, monto).
- `GET /api/pedidos/me/{id}` — detalle de un pedido propio puntual (para abrir uno de esas listas).
- `PATCH /api/cadetes/me/password`, `.../telefono`, `.../cuenta` — el cadete edita esto
  desde su Perfil; antes solo el admin podía tocar estos datos (y `cuenta`/CBU no existía).
- `/queue/cadete/{id}/avisos` (WebSocket) — avisos generales del admin y recordatorios
  de demora (`PedidoService.avisosDemora`), separado de `/viajes` y `/chat`.

También cambió el contrato de dos endpoints que ya existían, a pedido:

- `POST /pedidos/me/{id}/recepcion` (botón "Retirado"): `RecepcionRequest.fotoUrl` pasó
  de obligatorio a **opcional**, y ahora graba `Pedido.retiradoEn` (timestamp) sin
  importar si mandaron foto o no — antes solo guardaba la foto y no dejaba ningún
  registro de "retirado" si no se sacaba ninguna. `retiradoEn` se agregó también a
  `PedidoResponse` para que el panel admin lo pueda mostrar.
- `POST /pedidos/me/{id}/finalizar`: antes alcanzaba con **uno** de los dos
  (`receptorNombre` o `fotoUrl`); ahora el cadete tiene que mandar **los dos**
  obligatoriamente (`PedidoService.finalizarInterno`, parámetro `exigirComprobante`).
  El cierre manual del admin (botón "Finalizar" del dashboard, para cuando el cadete
  no tiene internet) sigue sin exigir ninguno de los dos — es una excepción a propósito.

Si cambiás algún DTO en el backend (agregás/sacás un campo de `CadeteResponse`,
`PedidoResponse`, etc.), actualizá el `data class` correspondiente en
`data/remote/dto/` — Gson no avisa en tiempo de compilación si dejan de coincidir,
solo en runtime (campo `null` o ausente).

## Simplificaciones conscientes (documentadas, no bugs)

- **Sin cuenta regresiva exacta** en la pantalla de "viaje nuevo, aceptar/rechazar":
  el backend no expone `expira_en` de la oferta en `PedidoResponse` (solo lo usa
  internamente para el job de reasignación). La app solo avisa "respondé pronto".
  Se puede sumar después si hace falta agregando ese campo a `PedidoResponse` en el
  backend.
- **Foto de cámara en baja resolución**: se usa
  `ActivityResultContracts.TakePicturePreview()` (devuelve un thumbnail `Bitmap`
  directo, sin `FileProvider` ni permisos de almacenamiento) en vez de guardar en
  resolución completa. Para el caso de uso (verificar dónde/a quién se entregó) alcanza
  y evita bastante configuración; si hace falta más calidad más adelante, cambiar a
  `ActivityResultContracts.TakePicture()` con un `FileProvider`.
- **`ACCESS_BACKGROUND_LOCATION` no se pide**: no hace falta — la app manda ubicación
  desde un foreground service real (notificación visible mientras está "Libre"), y ese
  modo no requiere el permiso de ubicación en segundo plano (ese permiso es para leer
  ubicación *fuera* de un foreground service). Si en algún momento se quisiera mandar
  ubicación sin que el cadete tenga la app abierta ni la notificación puesta, ahí sí
  haría falta sumarlo.
- **Reinicio del teléfono**: si el cadete reinicia el celular, tiene que volver a abrir
  la app y tocar "Libre" — no hay un `BroadcastReceiver` de `BOOT_COMPLETED` que
  reinicie el service solo. No estaba pedido en la spec; se puede sumar si en la
  práctica resulta molesto.

## Primer build 

Este proyecto **no incluye el `.jar` del wrapper de Gradle** (es un binario — no tiene
sentido versionarlo escrito a mano). Al abrir la carpeta `cadete-app/` en Android
Studio, va a ofrecer regenerarlo solo ("Gradle Sync" lo resuelve). Si preferís la
terminal y tenés Gradle instalado en el sistema:

```
cd cadete-app
gradle wrapper --gradle-version 8.7
```

Después:

1. Abrí `cadete-app/` como proyecto en Android Studio (versión Iguana/2023.2.1 o más
   nueva, para Kotlin 1.9.24 + AGP 8.5.2).
2. Dejá que sincronice — va a bajar todas las dependencias de Google/Maven Central.
3. **Sin `google-services.json` el proyecto compila igual** (el push queda
   deshabilitado, ver abajo) — para probar el flujo completo primero, no hace falta
   Firebase todavía.
4. Corré la app en un emulador o dispositivo con `minSdk 24`+.
5. En la primera pantalla, cargá la URL del backend (ej. `http://192.168.1.50:8080`
   si el backend corre en la misma red que el celular — **no sirve `localhost`** desde
   un dispositivo físico ni desde la mayoría de los emuladores; el emulador estándar
   de Android Studio usa `http://10.0.2.2:8080` para llegar al `localhost` de la PC
   host, que es el default en `gradle.properties`).

## Push (Firebase)

1. Entrá a [Firebase Console](https://console.firebase.google.com/), creá un proyecto
   (o usá el mismo de "otro proyecto del equipo" que menciona la spec 2, si ya existe
   uno con el mismo esquema).
2. Agregá una app Android con `applicationId` **`com.cadeteria.cadete`**.
3. Descargá el `google-services.json` generado y ponelo en `cadete-app/app/`.
4. Volvé a sincronizar el proyecto — a partir de ahí `app/build.gradle.kts` aplica
   solo el plugin de Google Services (ver el `if (hasGoogleServices)` ahí) y el push
   queda activo.
5. Del lado del backend, hace falta la cuenta de servicio de Firebase Admin — ver
   `app.fcm.credenciales-path` en `cadeteria/src/main/resources/application.yml`
   (`FcmService.java` ya está armado, solo le falta el JSON de credenciales para
   habilitarse).

Sin este paso, la app funciona igual mientras esté abierta (todo pasa por WebSocket);
lo único que no llega es el aviso cuando está cerrada o en background.

## Firma y distribución (Bluetooth, spec 2)

Para repartir la APK a los cadetes (hasta 30, sin Play Store):

1. Generá un build de release firmado desde Android Studio: *Build → Generate Signed
   Bundle / APK → APK*, creando (o reusando) un keystore.
2. Copiá el `.apk` resultante al celular por Bluetooth (o cualquier medio) y
   confirmá "Instalar de fuentes desconocidas" cuando lo pida.
3. En cada dispositivo, al abrir la app por primera vez, cargá la URL del backend real
   (no hace falta recompilar por cadete — ver "Configuración del servidor" arriba).

Guardá el keystore y su contraseña en un lugar seguro fuera del repo — sin él no se
pueden firmar actualizaciones futuras de la misma app (Android exige la misma firma
para instalar un APK "encima" de uno ya instalado).

## Qué falta probar (checklist para quien tenga Android Studio a mano)

- [ ] Compila y sincroniza sin errores tras `gradle wrapper` / apertura en Studio.
- [ ] Login contra un backend real (ver "Primer build" para la URL correcta según
      emulador vs. dispositivo físico).
- [ ] Toggle Libre/Desconectado dispara el foreground service (aparece la
      notificación persistente) y el cadete se ve moviéndose en el mapa del panel
      admin (`/mapa`).
- [ ] Asignar un pedido desde el panel admin → llega el viaje a la app (por WebSocket
      con la app abierta; por push si está en background, una vez configurado
      Firebase).
- [ ] Aceptar / rechazar; probar el caso de "se venció la oferta" (dejar pasar el
      `tiempo_limite_aceptacion_seg` configurado) y confirmar que la app se entera
      (evento `VIAJE_QUITADO`) — este es el bug de la sección 4 de la spec.
- [ ] Botón "Retirado" (con y sin foto) y confirmar que el panel admin lo refleja.
- [ ] Finalizar exige nombre y apellido de quien recibió **y** una foto — confirmar que
      el botón de confirmar del diálogo queda deshabilitado hasta completar ambos.
- [ ] Fotos de retiro/entrega: confirmar que suben a Cloudinary y el panel admin las ve
      (requiere Cloudinary configurado en Configuración, panel admin).
- [ ] Chat en ambas direcciones, con la app en foreground y en background.
- [ ] Probar en un dispositivo viejo real (Galaxy J7 o similar, Android 7/8) — es el
      piso de compatibilidad del spec y donde más pueden aparecer sorpresas de
      rendimiento/memoria con Compose + osmdroid.
