import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets.getByName("commonMain").dependencies {
        libs.findLibrary("kotlinx-coroutines-core").ifPresent { implementation(it) }
    }

    sourceSets.getByName("commonTest").dependencies {
        implementation(kotlin("test"))
        libs.findLibrary("kotlinx-coroutines-test").ifPresent { implementation(it) }
    }

    sourceSets.getByName("jvmTest").dependencies {
        libs.findLibrary("junit").ifPresent { implementation(it) }
    }
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
