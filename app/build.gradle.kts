plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-ksp") // Room için KSP plugin'i
}

android {
    namespace = "com.example.quizduellosu"
    compileSdk = 34 // En son SDK sürümünü kullanın

    defaultConfig {
        applicationId = "com.example.quizduellosu"
        minSdk = 24 // API 24 (Nougat)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Compose compiler versiyonunu kontrol et
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room (Yerel Veritabanı)
    val room_version = "2.6.1" // En son stabil sürümü kontrol et
    implementation("androidx.room:room-runtime:$room_version")
    // annotationProcessor("androidx.room:room-compiler:$room_version") // KSP kullanıldığı için bu satıra gerek yok
    ksp("androidx.room:room-compiler:$room_version")
    // Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // Firebase BoM (Bill of Materials) - Önerilen yöntem
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // En son BoM sürümünü kontrol et

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    // Firebase Firestore (Alternatif olarak Realtime Database yerine kullanılabilir)
    // implementation("com.google.firebase:firebase-firestore-ktx")

    // Firebase Authentication (Eğer oyuncu kimlik doğrulaması eklenecekse)
    implementation("com.google.firebase:firebase-auth-ktx")

    // Navigation Compose
    val nav_version = "2.7.7" // En son stabil sürümü kontrol et
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Jetpack Compose (Zaten yukarıda activity.compose ile eklenmiş olmalı, versiyonları kontrol edelim)
    // Compose BoM zaten yukarıda platform() ile ekli. Diğerleri de standart.
    // implementation(platform("androidx.compose:compose-bom:2024.02.02")) // En son BoM sürümünü kontrol et
    // implementation("androidx.compose.ui:ui")
    // implementation("androidx.compose.ui:ui-graphics")
    // implementation("androidx.compose.ui:ui-tooling-preview")
    // implementation("androidx.compose.material3:material3")
    // androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.02"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")
}
