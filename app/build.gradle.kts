plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.seminariofinal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.seminariofinal"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Configuración para permitir la desalineación de 16 KB en librerías nativas antiguas (.so)
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
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
}

dependencies {

    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Lazysodium para Android
    implementation("com.goterl:lazysodium-android:5.1.0@aar")
    // Java Native Access (JNA)
    implementation("net.java.dev.jna:jna:5.14.0@aar")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp: fija una sola versión para todos los módulos
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")  // JavaNetCookieJar (cookies de sesión)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // WebRTC (lo usa CallActivity)
    implementation("io.github.webrtc-sdk:android:144.7559.09")

}