import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

configure<ApplicationExtension> {
    namespace = "ua.ztr.bmsmonitor"
    compileSdk = 37

    defaultConfig {
        applicationId = "ua.ztr.bmsmonitor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


}

dependencies {
    "implementation"(project(":bms-ble"))

    "implementation"(platform("androidx.compose:compose-bom:2026.09.00"))
    "implementation"("androidx.compose.ui:ui")
    "implementation"("androidx.compose.ui:ui-graphics")
    "implementation"("androidx.compose.ui:ui-tooling-preview")
    "implementation"("androidx.compose.material3:material3")
    "implementation"("androidx.compose.material:material-icons-extended")
    "implementation"("androidx.activity:activity-compose:1.13.0")
    "implementation"("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    "implementation"("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    "implementation"("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    "implementation"("androidx.core:core-ktx:1.19.0")

    "debugImplementation"("androidx.compose.ui:ui-tooling")
}
