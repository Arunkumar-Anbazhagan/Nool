plugins {
    id("nool.kmp.library")
    id("app.cash.sqldelight")
}

android {
    namespace = "com.nool.core.index"
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":shared:core-model"))
        implementation(project(":shared:core-markdown"))
        libs.findLibrary("sqldelight-primitive-adapters").ifPresent { implementation(it) }
        libs.findLibrary("sqldelight-coroutines-extensions").ifPresent { implementation(it) }
    }
    sourceSets.getByName("androidMain").dependencies {
        libs.findLibrary("sqldelight-android-driver").ifPresent { implementation(it) }
    }
    sourceSets.getByName("jvmMain").dependencies {
        libs.findLibrary("sqldelight-sqlite-driver").ifPresent { implementation(it) }
    }
}

sqldelight {
    databases {
        create("NoolDatabase") {
            packageName.set("com.nool.core.index.db")
        }
    }
}
