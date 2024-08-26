plugins {
    id("com.android.library")  // Added version number
    id("org.jetbrains.kotlin.android") version "1.9.0" // Added version number
    id("maven-publish")
}

android {
    namespace = "com.transactpay.transactpay_android"
    compileSdk = 34

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    // Wrapper task moved outside of android block
    tasks.register<Wrapper>("wrapper") {
        gradleVersion = "8.10" // Specify the Gradle version you want to use
    }

    lint {
        targetSdk = 34
    }

    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.Omamuli-Emmanuel"
                artifactId = "pay_with_transact_pay"
                version = "0.0.1"

                pom {
                    name.set("Transactpay Native Android SDK")
                    description.set("Native Android SDK for Transactpay, built with Kotlin")
                    url.set("https://github.com/Omamuli-Emmanuel/paywithtransactpay")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("Omamuli-Emmanuel")
                            name.set("Emmanuel Omamuli")
                            email.set("omamuli.emmanuel@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:github.com/Omamuli-Emmanuel/paywithtransactpay.git")
                        developerConnection.set("scm:git@github.com:Omamuli-Emmanuel/paywithtransactpay.git")
                        url.set("https://github.com/Omamuli-Emmanuel/paywithtransactpay")
                    }
                }
            }
        }

        repositories {
            maven {
                url = uri("https://jitpack.io")
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("javax.xml.parsers:jaxp-api:1.4.5")
    implementation("com.squareup.picasso:picasso:2.8")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
