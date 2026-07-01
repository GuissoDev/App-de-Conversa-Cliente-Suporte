plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.lordseg"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.lordseg"
        minSdk = 26
        targetSdk = 37
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

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.okhttp)
    implementation(libs.material)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    // Motor central do ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.1.1")
// O Tradutor mágico para câmeras IP (RTSP)
    implementation("androidx.media3:media3-exoplayer-rtsp:1.1.1")
// Os componentes de interface de usuário (a tela do vídeo)
    implementation("androidx.media3:media3-ui:1.1.1")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
