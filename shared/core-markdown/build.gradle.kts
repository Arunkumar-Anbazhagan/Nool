plugins {
    id("nool.kmp.library")
}

android {
    namespace = "com.nool.core.markdown"
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":shared:core-model"))
        libs.findLibrary("commonmark").ifPresent { implementation(it) }
        libs.findLibrary("commonmark-ext-gfm-tables").ifPresent { implementation(it) }
        libs.findLibrary("commonmark-ext-gfm-strikethrough").ifPresent { implementation(it) }
    }
}
