import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }
    return properties.getProperty(key) ?: throw IllegalArgumentException("Property $key not found")
}

fun getReleaseProperty(key: String): String {
    val properties = Properties()
    val file = rootProject.file("release.properties")
    if (file.exists()) {
        properties.load(file.inputStream())
    } else {
        throw GradleException("Missing release.properties")
    }
    return properties.getProperty(key)
}


android {
    namespace = "com.example.awesomephotoframe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.awesomephotoframe"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }


    signingConfigs {
        create("release") {
            storeFile = file(getReleaseProperty("STORE_FILE"))
            storePassword = getReleaseProperty("STORE_PASSWORD")
            keyAlias = getReleaseProperty("KEY_ALIAS")
            keyPassword = getReleaseProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${getReleaseProperty("GOOGLE_CLIENT_ID")}\"")
            buildConfigField("String", "GOOGLE_CLIENT_SECRET", "\"${getReleaseProperty("GOOGLE_CLIENT_SECRET")}\"")
        }

        getByName("debug") {
            buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${getLocalProperty("GOOGLE_CLIENT_ID")}\"")
            buildConfigField("String", "GOOGLE_CLIENT_SECRET", "\"${getLocalProperty("GOOGLE_CLIENT_SECRET")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packagingOptions {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)

    // AdMob (Google 広告 SDK)
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation ("com.google.api-client:google-api-client-android:1.33.2")
    implementation ("com.google.http-client:google-http-client-gson:1.43.3")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.20.0")
    implementation ("androidx.activity:activity-ktx:1.7.2")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation ("com.google.photos.library:google-photos-library-client:1.7.3")
    implementation("com.google.api-client:google-api-client-android:1.35.2")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // 課金用
    implementation("com.android.billingclient:billing-ktx:6.1.0")
}