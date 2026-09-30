plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.marketintelligence"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.marketintelligence"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "TD_API_KEY", "\"${localProperties.getProperty("TD_API_KEY", "")}\"")
        buildConfigField("String", "ALPACA_API_KEY", "\"${localProperties.getProperty("ALPACA_API_KEY", "")}\"")
        buildConfigField("String", "ALPACA_SECRET_KEY", "\"${localProperties.getProperty("ALPACA_SECRET_KEY", "")}\"")
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String? ?: "key0"
            keyPassword = keystoreProperties["keyPassword"] as String? ?: "password"
            storeFile = file(keystoreProperties["storeFile"] as String? ?: "release.keystore")
            storePassword = keystoreProperties["storePassword"] as String? ?: "password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }

    packagingOptions {
        exclude("org/apache/commons/codec/language/bm/gen_rules_dutch.txt")
        exclude("org/apache/commons/codec/language/bm/gen_approx_greeklatin.txt")
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":trade-engine"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.32.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")

    implementation("com.amplifyframework:core:2.14.0")
    implementation("com.amplifyframework:aws-api:2.14.0")

    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")

    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-okhttp:2.3.10")
    implementation("io.ktor:ktor-client-websockets:2.3.10")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3") {
            because("Ensure all serialization libraries are on the same version")
        }
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") {
            because("Ensure all serialization libraries are on the same version")
        }
    }
}

tasks.register("generateKeystore") {
    doLast {
        val keystoreFile = file("release.keystore")
        if (!keystoreFile.exists()) {
            println("Generating keystore...")
            ant.withGroovyBuilder {
                "genkey"(
                    mapOf(
                        "alias" to "key0",
                        "storepass" to "marketintelligence",
                        "keypass" to "marketintelligence",
                        "keystore" to keystoreFile.absolutePath,
                        "dname" to "CN=Market Intelligence, OU=Engineering, O=Market Intelligence, L=City, S=State, C=US",
                        "validity" to 10000,
                        "keyalg" to "RSA",
                        "keysize" to 2048,
                        "storetype" to "JKS"
                    )
                )
            }
            println("Keystore generated at ${keystoreFile.absolutePath}")
        } else {
             println("Keystore already exists.")
        }
    }
}
