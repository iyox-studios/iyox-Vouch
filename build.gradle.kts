import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.qodana)
}

qodana {
}

tasks.named<org.jetbrains.qodana.tasks.QodanaScanTask>("qodanaScan") {
    val qodanaLocalProperties = layout.projectDirectory.file(".qodana/local.properties").asFile
    val qodanaInnerGradle = layout.buildDirectory.dir("qodana/inner-gradle").get().asFile
    val qodanaInnerIdea = layout.buildDirectory.dir("qodana/inner-idea").get().asFile
    val localProperties = Properties().apply {
        val propertiesFile = layout.projectDirectory.file("local.properties").asFile
        if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
    }
    val androidSdk = localProperties.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
    doFirst {
        qodanaLocalProperties.parentFile.mkdirs()
        qodanaLocalProperties.writeText("sdk.dir=/opt/android-sdk\n")
        qodanaInnerGradle.mkdirs()
        qodanaInnerIdea.mkdirs()
    }
    arguments.set(buildList {
        add("--volume=${gradle.gradleUserHomeDir}/wrapper:/root/.gradle/wrapper:ro")
        add("--volume=${qodanaLocalProperties.absolutePath}:/data/project/local.properties")
        add("--volume=${qodanaInnerGradle.absolutePath}:/data/project/.gradle")
        // Keep the container's project model separate from Android Studio's host paths.
        add("--volume=${qodanaInnerIdea.absolutePath}:/data/project/.idea")
        if (androidSdk != null) {
            add("--volume=$androidSdk:/opt/android-sdk:ro")
        }
    })
}
