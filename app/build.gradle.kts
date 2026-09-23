plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// El plugin de Firebase necesita google-services.json para poder aplicarse. Sin el
// archivo, el proyecto compila igual (el push queda deshabilitado, ver
// CadeteFirebaseMessagingService.kt) — así el build no se rompe antes de configurar
// Firebase. Ver README.md, sección "Push (Firebase)".
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.cadeteria.cadete"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cadeteria.cadete"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        val defaultBaseUrl = project.findProperty("CADETE_APP_DEFAULT_BASE_URL") as String?
            ?: "http://10.0.2.2:8080"
        buildConfigField("String", "DEFAULT_BASE_URL", "\"$defaultBaseUrl\"")
        buildConfigField("boolean", "HAS_GOOGLE_SERVICES", hasGoogleServices.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    // Leer/corregir la orientación EXIF de las fotos de retiro/entrega (cámara en TakePicture()).
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose UI (Material 3)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navegación
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Persistencia liviana de sesión (token, base URL)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash screen con el logo de marca al abrir la app (API 31+ nativo, compat para atrás).
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Red: Retrofit + OkHttp (REST) — el cliente STOMP (realtime/StompClient.kt) se
    // construye a mano sobre el WebSocket de OkHttp, sin librerías de terceros.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Ubicación en background (foreground service)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Push
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Mapa (mismo proveedor que el front admin: OpenStreetMap, acá vía osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Carga de imágenes (foto de perfil/vehículo, vienen de Cloudinary)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Widget de pantalla de inicio para activarse/desactivarse sin abrir la app (mejora 2026-09-16)
    implementation("androidx.glance:glance-appwidget:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
