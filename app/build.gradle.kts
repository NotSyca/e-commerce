plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.example.e_commerce"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.e_commerce"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"https://shsyupdqneeolmbhhmof.supabase.co/\"")
        buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNoc3l1cGRxbmVlb2xtYmhobW9mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAyODc3OTAsImV4cCI6MjA3NTg2Mzc5MH0.6jRGAzOD6uyxiGlj--5Ewwpk-iacqc9ZGeHBLvbk4eY\"")
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
        buildConfig = true
    }
    
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildToolsVersion = "36.0.0"
}

dependencies {

    // LIBRERÍAS DE UI Y COMPATIBILIDAD
    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation(libs.androidx.viewpager2) // Necesario para ViewPager2

    // SUPABASE (RESTAURADO: Usando el prefijo antiguo que tu código espera)
    // CRÍTICO: Mantenemos el prefijo 'jan-tennert' para que tus imports existentes funcionen.
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.5.2")

    // GLIDE (RESTAURADO: Versión estable 4.x + annotationProcessor)
    // Si usas 5.0.5, el compilador debe ser KSP, pero la 4.x es más fácil de configurar.
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.activity)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // SERIALIZACIÓN Y HTTP
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation(libs.kotlinx.coroutines.android) // Corrutinas

    // OTRAS LIBRERÍAS QUE ESTABAN PRESENTES (Restauradas por si son requeridas por otras partes del código)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.retrofit)
    implementation(libs.converter.gson) // Para TinyDB y compatibilidad de JSON
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // implementation(libs.coil) // Coil es opcional si solo usas Glide

    // TESTING
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Coil
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-base:2.6.0")
}