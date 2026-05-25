import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val envProps = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    } else {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }
}

android {
    namespace = "com.podut.dataagregate"
    compileSdk = libs.versions.compileSdk.get().toInt()

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            val baseUrl = envProps.getProperty("API_BASE_URL")
                ?: System.getenv("API_BASE_URL")
                ?: "http://pixcode.go.ro:8085"
            buildConfigField("String", "API_BASE_URL", "\"$baseUrl\"")
            applicationIdSuffix = ".dev"
        }
        create("prod") {
            dimension = "env"
            val baseUrl = envProps.getProperty("API_BASE_URL_PROD")
                ?: System.getenv("API_BASE_URL_PROD")
                ?: "https://apiagregate.petrupodut.dev"
            buildConfigField("String", "API_BASE_URL", "\"$baseUrl\"")
        }
    }

    defaultConfig {
        applicationId = "com.podut.dataagregate"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile     = File(envProps.getProperty("KEYSTORE_PATH", ""))
            storePassword = envProps.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias      = envProps.getProperty("KEY_ALIAS", "")
            keyPassword   = envProps.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose     = true
        buildConfig = true
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":feature:news_feed"))
    implementation(project(":feature:explore"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:category_articles"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:article_reader"))
    implementation(project(":feature:saved"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
