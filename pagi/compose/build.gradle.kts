plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "app.thamani.pagi.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = findProperty("GROUP").toString()
                artifactId = "pagi-compose"
                version = findProperty("VERSION_NAME").toString()

                pom {
                    name.set("Pagi Compose")
                    description.set("Jetpack Compose integration for the Pagi pagination library")
                    url.set("https://github.com/AzizAfworker/Pagi")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(project(":pagi:core"))
    implementation(libs.kotlinx.coroutines.core)
    api(platform(libs.androidx.compose.bom))
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.runtime:runtime")
    implementation(libs.androidx.compose.ui)
}
