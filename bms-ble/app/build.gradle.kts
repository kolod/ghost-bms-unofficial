import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Локальний ключ для підпису release-збірки — генерується один раз (keytool),
// файли keystore.properties/*.keystore.jks у .gitignore, у репозиторій не потрапляють.
val keystoreProperties = Properties().apply {
    val propsFile = file("keystore.properties")
    if (propsFile.exists()) load(propsFile.inputStream())
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

    if (keystoreProperties.containsKey("storeFile")) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
