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
    doFirst {
        qodanaLocalProperties.parentFile.mkdirs()
        qodanaLocalProperties.writeText("sdk.dir=/opt/android-sdk\n")
        qodanaInnerGradle.mkdirs()
    }
    arguments.set(listOf(
        "--volume=${gradle.gradleUserHomeDir}/wrapper:/root/.gradle/wrapper:ro",
        "--volume=${qodanaLocalProperties.absolutePath}:/data/project/local.properties",
        "--volume=${qodanaInnerGradle.absolutePath}:/data/project/.gradle"
    ))
}