import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
}

configure<LibraryExtension> {
    namespace = "ua.ztr.bmsble"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    "testImplementation"("junit:junit:4.13.2")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
