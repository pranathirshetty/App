import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kmpAppIconGenerator)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // Ktor HTTP client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)
            // DataStore for session persistence
            implementation(libs.datastore.preferences.core)
            // Image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            // Date/time
            implementation(libs.kotlinx.datetime)
            // Okio (path support for DataStore)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.rxffmpeg)
            
            // Native libmpv for Android (has ASS support via libass)
            implementation("dev.jdtech.mpv:libmpv:0.5.1")
            implementation("net.java.dev.jna:jna:5.14.0@aar")
            
            // MediaSession for earphone/headphone media button support
            implementation(libs.androidx.media3.session)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            // CIO engine for desktop HTTP
            implementation(libs.ktor.client.cio)
            // JNA for getting AWT canvas WID for MPV playback
            implementation(libs.jna)
            
            // JNativeHook for cross-platform global media keys (earphone play/pause)
            implementation(libs.jnativehook)
            implementation(libs.dbus.java)
            implementation(libs.jave.all.deps)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "to.kuudere.anisuge"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Add repositories block here if needed, but android block doesn't usually take it.
    // Wait, repositories should be in the top level or settings.
    
    defaultConfig {
        applicationId = "to.kuudere.anisuge"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

compose.desktop {
    application {
        mainClass = "to.kuudere.anisuge.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "AnisugKMP"
            packageVersion = "1.0.0"
            description = "Anisuge — KMP Edition"
            copyright = "© 2026 Kuudere"
            vendor = "Kuudere"

            linux {
                iconFile.set(project.file("src/desktopMain/resources/logo.png"))
            }
        }
    }
}


