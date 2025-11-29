plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // El plugin KSP es vital para Room, lo mantenemos
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.levelup_gamerpractica"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.levelup_gamerpractica"
        minSdk = 33 // Puedes bajarlo a 26 si quieres soportar mas telefonos, pero 33 esta bien para practica
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- TESTING ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- IMÁGENES (COIL) ---
    // Eliminé la línea duplicada hardcoded, usamos la del catálogo
    implementation(libs.coil.compose)

    // --- RED (RETROFIT & GSON) ---
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Agregamos OkHttp explícitamente para asegurar que el AuthInterceptor funcione bien
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // --- UI & COMPOSE ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Iconos extendidos (Vital para iconos como ShoppingCartCheckout)
    implementation(libs.androidx.compose.material.icons.extended)

    // --- NAVEGACIÓN & LIFECYCLE ---
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // --- UTILS ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    // DataStore (Lo tenías agregado, lo dejamos por si acaso, aunque usamos SharedPreferences)
    implementation(libs.androidx.datastore.preferences)

    // --- BASE DE DATOS LOCAL (ROOM) ---
    // Eliminé la variable "room_version" porque usas "libs" y no la estabas usando.
    implementation(libs.androidx.room.room.runtime)
    implementation(libs.androidx.room.ktx) // Vital para usar corrutinas en Room
    ksp(libs.androidx.room.compiler)      // Vital para generar el código de la BD
}