plugins {
    id("nool.kmp.library")
}

android {
    namespace = "com.nool.core.templates"
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":shared:core-model"))
        libs.findLibrary("kotlinx-datetime").ifPresent { api(it) }
    }
}
