plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aclass"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aclass"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Core Android libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.fragment:fragment:1.6.2")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.recyclerview:recyclerview-selection:1.2.0")

    // Navigation (if using Navigation Component)
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // Firebase

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.legacy.support.v4)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Cloud storage
    implementation("com.cloudinary:cloudinary-android:2.2.0")

    // AR Core
    implementation("com.google.ar:core:1.42.0")

    // PDF Viewer - Try multiple versions to see which works
    implementation ("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")    // Alternative if above doesn't work:
    // implementation("com.github.barteksc:android-pdf-viewer:2.8.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

apply(plugin = "com.google.gms.google-services")

