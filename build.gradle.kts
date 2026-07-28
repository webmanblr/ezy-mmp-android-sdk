plugins {
    id("com.android.library") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.20"
    id("maven-publish")
}

android {
    namespace = "io.ezyurl.mmp"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.ezyurl"
                artifactId = "ezymmp"
                version = "1.0.0"

                pom {
                    name.set("EzyMMP Android SDK")
                    description.set("A lightweight Android SDK for attributing app installs and tracking events back to EzyURL short links.")
                    url.set("https://github.com/webmanblr/ezy-mmp-android-sdk")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("ezyurl")
                            name.set("EzyURL Team")
                            email.set("support@ezyurl.io")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/webmanblr/ezy-mmp-android-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/webmanblr/ezy-mmp-android-sdk.git")
                        url.set("https://github.com/webmanblr/ezy-mmp-android-sdk")
                    }
                }
            }
        }
    }
}
