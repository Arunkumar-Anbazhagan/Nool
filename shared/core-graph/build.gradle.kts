plugins {
    id("nool.kmp.library")
}

android {
    namespace = "com.nool.core.graph"
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":shared:core-model"))
    }
}
