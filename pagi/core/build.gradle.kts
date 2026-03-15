plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = findProperty("GROUP").toString()
            artifactId = "pagi-core"
            version = findProperty("VERSION_NAME").toString()

            pom {
                name.set("Pagi Core")
                description.set("A lightweight, coroutine-first pagination library for Kotlin")
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

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
