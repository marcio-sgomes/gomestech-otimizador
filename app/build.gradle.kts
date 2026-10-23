plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Versão vem do número da compilação automática (GitHub Actions). Localmente fica 1.
val appVersionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
val updateRepo = (project.findProperty("updateRepo") as String?) ?: "marcio-sgomes/gomestech-otimizador"

// Assinatura: as chaves ficam nos "Secrets" do GitHub, nunca no código.
val keystorePath: String? = System.getenv("KEYSTORE_FILE")
val hasReleaseKey = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "br.com.gomestech.otimizador"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.gomestech.otimizador"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "1.0.$appVersionCode"
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        animationsDisabled = true
        unitTests.isReturnDefaultValues = false
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        // Relatório em app/build/reports/lint-results-debug.html
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Versão que as bibliotecas de teste esperam (evita erro "Trace.isEnabled" nos emuladores)
    implementation("androidx.tracing:tracing:1.1.0")

    // Testes da lógica (rodam no computador do GitHub)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    // Robô de vistoria (roda nos emuladores Android)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
