import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun loadEnvFile(path: String): Map<String, String> {
    val file = rootProject.file(path)
    if (!file.exists()) return emptyMap()

    return buildMap {
        file.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachLine
            val delimiter = line.indexOf('=')
            if (delimiter <= 0) return@forEachLine
            val key = line.substring(0, delimiter).trim()
            val value = line.substring(delimiter + 1).trim().removeSurrounding("\"")
            put(key, value)
        }
    }
}

val debugEnv = loadEnvFile(".env.development")
val releaseEnv = loadEnvFile(".env.production")

fun envValue(
    env: Map<String, String>,
    key: String,
    defaultValue: String,
): String = env[key]?.takeIf { it.isNotBlank() } ?: defaultValue

fun escapeGradleString(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

fun assetStatementsValue(originHint: String): String {
    val normalizedOrigin = runCatching {
        val uri = URI(originHint.trim())
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        if (scheme != "https" || host.isNullOrBlank()) {
            null
        } else {
            "$scheme://$host"
        }
    }.getOrNull()

    if (normalizedOrigin == null) {
        return "[]"
    }

    return """[{"include":"$normalizedOrigin/.well-known/assetlinks.json"}]"""
}

android {
    namespace = "cn.ksuser.auth.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cn.ksuser.auth.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val apiBaseUrl = envValue(debugEnv, "API_BASE_URL", "https://api.ksuser.cn")
            val passkeyRpId = envValue(debugEnv, "PASSKEY_RP_ID", "auth.ksuser.cn")
            val passkeyOriginHint = envValue(debugEnv, "PASSKEY_ORIGIN_HINT", "https://auth.ksuser.cn")
            buildConfigField("String", "API_BASE_URL", "\"${escapeGradleString(apiBaseUrl)}\"")
            buildConfigField("String", "PASSKEY_RP_ID", "\"${escapeGradleString(passkeyRpId)}\"")
            buildConfigField("String", "PASSKEY_ORIGIN_HINT", "\"${escapeGradleString(passkeyOriginHint)}\"")
            buildConfigField("String", "APP_ENV", "\"${envValue(debugEnv, "APP_ENV", "development")}\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", envValue(debugEnv, "ENABLE_HTTP_LOGGING", "true"))
            resValue("string", "asset_statements", assetStatementsValue(passkeyOriginHint))
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val apiBaseUrl = envValue(releaseEnv, "API_BASE_URL", "https://api.ksuser.cn")
            val passkeyRpId = envValue(releaseEnv, "PASSKEY_RP_ID", "auth.ksuser.cn")
            val passkeyOriginHint = envValue(releaseEnv, "PASSKEY_ORIGIN_HINT", "https://auth.ksuser.cn")
            buildConfigField("String", "API_BASE_URL", "\"${escapeGradleString(apiBaseUrl)}\"")
            buildConfigField("String", "PASSKEY_RP_ID", "\"${escapeGradleString(passkeyRpId)}\"")
            buildConfigField("String", "PASSKEY_ORIGIN_HINT", "\"${escapeGradleString(passkeyOriginHint)}\"")
            buildConfigField("String", "APP_ENV", "\"${envValue(releaseEnv, "APP_ENV", "production")}\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", envValue(releaseEnv, "ENABLE_HTTP_LOGGING", "false"))
            resValue("string", "asset_statements", assetStatementsValue(passkeyOriginHint))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.com.google.mlkit.barcode.scanning)
    testImplementation(libs.junit)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
