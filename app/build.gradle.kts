plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.calculator.gtypxz"
    minSdk = 24
    targetSdk = 36
    val inputVersionCode = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull()
      ?: (System.getenv("VERSION_CODE")?.toIntOrNull())
    versionCode = inputVersionCode ?: 19

    val inputVersionName = (project.findProperty("VERSION_NAME") as? String)
      ?: System.getenv("VERSION_NAME")
    versionName = inputVersionName ?: "19.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val storeFilePath = (project.findProperty("STORE_FILE") as? String)
      ?: System.getenv("STORE_FILE")
      ?: (project.findProperty("KEYSTORE_PATH") as? String)
      ?: System.getenv("KEYSTORE_PATH")

    val customFile = when {
      storeFilePath != null && file(storeFilePath).exists() -> file(storeFilePath)
      file("${rootDir}/upload-keystore.jks").exists() -> file("${rootDir}/upload-keystore.jks")
      file("${rootDir}/my-upload-key.jks").exists() -> file("${rootDir}/my-upload-key.jks")
      file("${rootDir}/debug.keystore").exists() -> file("${rootDir}/debug.keystore")
      else -> null
    }

    if (customFile != null) {
      create("release") {
        storeFile = customFile
        storePassword = (project.findProperty("STORE_PASSWORD") as? String)
          ?: System.getenv("STORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
        keyAlias = (project.findProperty("KEY_ALIAS") as? String)
          ?: System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "upload"
        keyPassword = (project.findProperty("KEY_PASSWORD") as? String)
          ?: System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
      }
    }
  }

  lint {
    abortOnError = true
    checkReleaseBuilds = true
    textReport = true
    textOutput = file("stdout")
    explainIssues = false
    warningsAsErrors = false
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseSigning = signingConfigs.findByName("release")
      if (releaseSigning != null) {
        signingConfig = releaseSigning
      }
    }
    debug {
      val debugSigning = signingConfigs.findByName("release")
      if (debugSigning != null) {
        signingConfig = debugSigning
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

tasks.register("testReleaseUnitTest") {
  description = "Runs unit tests for release build verification"
  dependsOn("test")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.play.integrity)
  implementation(libs.play.services.base)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
