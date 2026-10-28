plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.omegadex.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.omegadex.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric : tests Room et Android sur la JVM, sans émulateur.
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkDependencies = true
        // Les mises à jour de dépendances sont suivies à part, elles ne bloquent pas le build.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

room {
    // Schémas exportés et versionnés : ils servent aux tests de migration.
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java")
}

// detekt 1.23 embarque son propre compilateur Kotlin : on évite qu'il soit aligné sur la version du projet.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.21")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Argon2id (hachage des mots de passe), en Java pur
    implementation(libs.bouncycastle)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
}
