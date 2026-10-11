import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun prop(key: String): String = localProperties.getProperty(key, "") ?: ""

/** 天地图 tk：TIANDITU_TK_1..5，兼容旧单项 TIANDITU_TK。 */
fun tiandituKeysLiteral(): String {
    val numbered = (1..5).map { prop("TIANDITU_TK_$it") }.filter { it.isNotBlank() }
    val keys = when {
        numbered.isNotEmpty() -> numbered
        prop("TIANDITU_TK").isNotBlank() -> listOf(prop("TIANDITU_TK"))
        else -> emptyList()
    }
    return keys.joinToString(prefix = "{", postfix = "}") { "\"$it\"" }
}

android {
    namespace = "com.openbows.camera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.openbows.camera"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "1.5.13"
        buildConfigField("String[]", "TIANDITU_TKS", tiandituKeysLiteral())
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    val releaseStoreFile = prop("RELEASE_STORE_FILE")
    val releaseStorePassword = prop("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = prop("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = prop("RELEASE_KEY_PASSWORD")
    val releaseKeystore = if (releaseStoreFile.isNotBlank()) {
        rootProject.file(releaseStoreFile)
    } else {
        null
    }
    if (releaseKeystore != null && releaseKeystore.exists() &&
        releaseStorePassword.isNotBlank() && releaseKeyAlias.isNotBlank()
    ) {
        signingConfigs {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword.ifBlank { releaseStorePassword }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
