import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties es la fuente de configuración por entorno: SDK, Maps y
// Cloudinary. Nada de esto viaja al repo (ver .gitignore).
val localProperties: Properties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use(::load)
}

fun localProperty(name: String): String =
    (project.findProperty(name) as String?) ?: localProperties.getProperty(name) ?: ""

// API key de Google Maps: se toma de local.properties (MAPS_API_KEY=...) o de -PMAPS_API_KEY.
val mapsApiKey: String = localProperty("MAPS_API_KEY")

// Cloudinary: el nombre del cloud y el preset unsigned no son secretos (viajan
// en el APK por diseño), pero se leen igual de local.properties para que cada
// entorno apunte a su propia cuenta sin tocar el código.
val cloudinaryCloudName: String = localProperty("CLOUDINARY_CLOUD_NAME")
val cloudinaryUploadPreset: String = localProperty("CLOUDINARY_UPLOAD_PRESET")

// Firma del APK de release. El keystore y sus contraseñas viven en
// keystore.properties, que está en .gitignore. Sin ese archivo el release se
// arma sin firmar en vez de romper la compilación: quien clone el repo puede
// seguir compilando debug, pero solo quien tenga la llave puede entregar.
val keystoreProperties: Properties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) keystoreFile.inputStream().use(::load)
}

android {
    namespace = "com.example.aulasegura"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.aulasegura"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$cloudinaryUploadPreset\"")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Play Integrity identifica la app por el certificado que la firma:
            // este signingConfig es lo que hace que el APK entregado tenga el
            // SHA-256 registrado en Firebase, en lugar del keystore de debug.
            signingConfig = signingConfigs.findByName("release")
            // Sin optimización a propósito: este APK se entrega a compañeros y
            // todavía no hay una prueba en dispositivo que cubra el binario
            // reducido por R8. Se vuelve a activar cuando exista una matriz de
            // pruebas en teléfonos reales.
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Falso positivo de AGP 9 sobre un detector de androidx.activity: la app
        // es puramente Compose (MainActivity extiende ComponentActivity) y no
        // declara ningún Fragment, mientras que `fragment` llega transitivo
        // desde appcompat 1.7.1 y resuelve 1.5.4 — muy por encima del 1.3.0 que
        // el mensaje pide. El detector compara una coordenada vieja del modelo
        // de lint y marca el `registerForActivityResult` de MainActivity como
        // fatal. Se desactiva solo ese chequeo; el resto de lint vital sigue
        // rompiendo el build.
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

ksp {
    // Room exporta el esquema para poder escribir migraciones verificables.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.ai)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Coil 2 remains for the teacher screens (coil.compose imports); the admin
    // screens use Coil 3 (coil3.compose). Both groups coexist without clashing.
    implementation(libs.coil2.compose)
implementation(libs.zxing.core)
implementation(libs.datastore.preferences)
implementation(libs.camerax.core)
implementation(libs.camerax.camera2)
implementation(libs.camerax.lifecycle)
implementation(libs.camerax.view)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}