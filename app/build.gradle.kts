import java.security.KeyStore

plugins {
    id("com.android.application")
}

val releaseVersionName = (findProperty("versionNameOverride") as String?)?.takeIf { it.isNotBlank() }
val releaseVersionCode = (findProperty("versionCodeOverride") as String?)?.toIntOrNull()

fun signingValue(env: String): String? = System.getenv(env)?.takeIf { it.isNotBlank() }

val keystoreFile = signingValue("KEYSTORE_FILE")
val keystorePassword = signingValue("KEYSTORE_PASSWORD")
val keystoreAlias = signingValue("KEY_ALIAS") ?: keystoreFile?.let { path ->
    val f = file(path)
    if (f.isFile) {
        KeyStore.getInstance("PKCS12").run {
            f.inputStream().use { load(it, keystorePassword?.toCharArray()) }
            aliases().toList().firstOrNull()
        }
    } else null
}

android {
    namespace = "io.github.lcebot.miscannerenhanced"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.lcebot.miscannerenhanced"
        minSdk = 29
        targetSdk = 35
        versionCode = releaseVersionCode ?: 1000
        versionName = releaseVersionName ?: "1.0.0"
    }

    signingConfigs {
        if (keystoreAlias != null) create("release") {
            storeFile = file(keystoreFile!!)
            storeType = "PKCS12"
            storePassword = keystorePassword
            keyAlias = keystoreAlias
            keyPassword = signingValue("KEY_PASSWORD") ?: keystorePassword
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        release {
            if (keystoreAlias != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
}
