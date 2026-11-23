// Referencia: FUSIONA estas partes con el build.gradle.kts que genera Android Studio
// (no lo reemplaces entero: los plugins y versiones del tuyo deben coincidir con tu Studio).

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")        // Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.serialization")  // <-- NUEVO: mismo numero de version que Kotlin
}

android {
    namespace = "com.flanbross.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flanbross.app"
        minSdk = 26            // java.time sin desugaring
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // <-- NUEVO: direccion del servidor. 10.0.2.2 = el PC visto desde el emulador.
        // En un celular real: poner en gradle.properties  flanbross.apiUrl=http://IP-DE-TU-PC:8080/
        val api = (project.findProperty("flanbross.apiUrl") as String?) ?: "http://10.0.2.2:8080/"
        buildConfigField("String", "API_URL", "\"$api\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true     // <-- NUEVO: genera BuildConfig.API_URL
    }
}

dependencies {
    // <-- NUEVO / confirmar que esten (versiones de referencia; Studio puede sugerir mas recientes)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")                         // red + cookies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")     // JSON
    implementation("io.coil-kt:coil-compose:2.7.0")                              // imagenes
}
