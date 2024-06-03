plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.esp32test"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.esp32test"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //implementation ("com.android.support:support-v4:28.0.0'") // Adjust version as needed


    // OkHttp dependency
    implementation("com.squareup.okhttp3:okhttp:4.9.3") // CAN Replace with desired version

    implementation ("com.google.code.gson:gson:2.9.0")

    implementation ("io.socket:socket.io-client:2.0.1")

}