plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.gridguild"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gridguild"
        minSdk = 28
        targetSdk = 35
        versionCode = 16
        versionName = "0.14.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    useLibrary("android.car")
    testOptions { animationsDisabled = true }
    // Release credentials exist only in the invoking process, never in source control.
    val releaseStore = providers.environmentVariable("GRIDGUILD_SIGNING_STORE").orNull
    val releasePassword = providers.environmentVariable("GRIDGUILD_SIGNING_PASSWORD").orNull
    val hasReleaseSigning = !releaseStore.isNullOrBlank() && !releasePassword.isNullOrBlank()
    if (hasReleaseSigning) {
        signingConfigs.create("submission") {
            storeFile = file(releaseStore!!)
            storeType = "PKCS12"
            storePassword = (releasePassword)
            keyAlias = "gridguild"
            keyPassword = (releasePassword)
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("submission")
        }
    }
    gradle.taskGraph.whenReady {
        if (!hasReleaseSigning && allTasks.any { it.project == project && it.name.contains("Release") }) {
            throw GradleException("Release signing is required. Use scripts/build-release.ps1; see README.md.")
        }
    }
}
dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
