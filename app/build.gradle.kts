import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.fidriyanto.banktracker"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.fidriyanto.banktracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.fidriyanto.banktracker.HiltTestRunner"
        buildConfigField("String", "API_BASE_URL", "\"${localProps.getProperty("API_BASE_URL", "").trimEnd('/')}\"")
        buildConfigField("String", "API_TOKEN", "\"${localProps.getProperty("API_TOKEN", "")}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Lets plain unit tests run code that logs with android.util.Log
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging {
        resources.excludes.add("META-INF/*")
    }
}

// Hilt 2.57 reads class metadata up to Kotlin 2.3. Kotlin 2.4 writes 2.4. Remove this on Hilt 2.60 and AGP 9.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.google.fonts)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.compose.material.icons.extended)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.security.crypto)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.litertlm)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.test.runner)
}

// Scoped to whitespace/indentation/spacing, not a full house-style rewrite (trailing commas,
// forced line-wrapping, naming conventions, brace insertion) — see the comment in .editorconfig.
val ktlintEditorConfig = mapOf(
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "off",
    "ktlint_standard" to "disabled",
    "ktlint_standard_indent" to "enabled",
    "ktlint_standard_final-newline" to "enabled",
    "ktlint_standard_no-trailing-whitespaces" to "enabled",
    "ktlint_standard_no-multi-spaces" to "enabled",
    "ktlint_standard_no-consecutive-blank-lines" to "enabled",
    "ktlint_standard_no-blank-line-before-rbrace" to "enabled",
    "ktlint_standard_no-empty-first-line-in-class-body" to "enabled",
    "ktlint_standard_no-leading-empty-lines-in-method-blocks" to "enabled",
    "ktlint_standard_spacing" to "enabled",
    "ktlint_standard_no-semicolons" to "enabled",
    "ktlint_standard_string-template-indent" to "enabled",
)

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint("1.8.0").editorConfigOverride(ktlintEditorConfig)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.8.0").editorConfigOverride(ktlintEditorConfig)
    }
}
