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
    namespace = "io.github.kolod.ghostbms"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.kolod.ghostbms"
        minSdk = 26
        targetSdk = 37
        // CI passes -PappVersionCode/-PappVersionName from the release tag; local/debug builds fall back to these.
        versionCode = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = findProperty("appVersionName") as String? ?: "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Реєструє res/values-*/ мови в locales_config.xml, щоб застосунок з'явився
    // в системних Налаштування → Мови застосунків (Android 13+).
    androidResources {
        generateLocaleConfig = true
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
    }

    buildTypes {
        getByName("release") {
            // Strips unused code and obfuscates class/method names — without this the
            // release APK decompiles back to near-original source (see proguard-rules.pro).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro"),
            )
            if (keystoreProperties.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    "implementation"(project(":bms-ble"))

    // Дає android:theme значення Theme.Material3.DayNight.* — саме та тема, під яку
    // узгоджено ui/Theme.kt (light/darkColorScheme), тому вікно не блимає світлим фоном
    // перед першим кадром Compose у темному режимі.
    "implementation"("com.google.android.material:material:1.14.0")

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
