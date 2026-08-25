    plugins {
        alias(libs.plugins.android.library)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.kotlin.compose)
    }

    android {
        namespace = "app.auriel.basalt.feature.stopwatch"
        compileSdk = 36

        defaultConfig {
            minSdk = 29
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
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.core.ktx)
        implementation(libs.kotlinx.coroutines.android)
        implementation(project(":core:design"))
implementation(project(":core:time"))
implementation(project(":core:data"))
implementation(libs.androidx.lifecycle.viewmodel.compose)

        testImplementation(libs.junit)
    }

