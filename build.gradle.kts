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
    arguments.set(listOf(
        "--volume=${gradle.gradleUserHomeDir}/wrapper:/root/.gradle/wrapper:ro"
    ))
}