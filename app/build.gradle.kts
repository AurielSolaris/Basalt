import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing credentials.
 *
 * Read from `keystore.properties` (git-ignored) with an environment-variable
 * fallback so CI can supply them without a file on disk. Neither the keystore
 * nor the passwords belong in version control, so a missing configuration is
 * not an error — the release build simply goes unsigned, and says so.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun secret(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile: String? = secret("storeFile", "BASALT_KEYSTORE")
val releaseStorePassword: String? = secret("storePassword", "BASALT_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = secret("keyAlias", "BASALT_KEY_ALIAS")
val releaseKeyPassword: String? = secret("keyPassword", "BASALT_KEY_PASSWORD")
val canSignRelease = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword,
).all { !it.isNullOrBlank() } && rootProject.file(releaseStoreFile!!).exists()

android {
    namespace = "app.auriel.basalt"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.auriel.basalt"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "Basalt: no release signing configured — see keystore.properties.template. " +
                        "assembleRelease will produce an unsigned APK.",
                )
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
    }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:dotmatrix"))
    implementation(project(":core:time"))
    implementation(project(":core:data"))
    implementation(project(":feature:alarm"))
    implementation(project(":feature:clock"))
    implementation(project(":feature:timer"))
    implementation(project(":feature:stopwatch"))
    implementation(project(":feature:bedtime"))
    implementation(project(":feature:settings"))
    implementation(project(":widget"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
