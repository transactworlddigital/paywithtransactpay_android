plugins {
    id("com.android.library")
    id("maven-publish")
    id("kotlin-android")
}

android {
    namespace = "com.transactpay.transactpay_android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }

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

    lint {
        targetSdk = 34
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.register<Wrapper>("wrapper") {
    gradleVersion = "8.1"
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
                    url.set("https://github.com/Omamuli-Emmanuel/paywithtransactpay_android")

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
                        connection.set("scm:git@github.com:Omamuli-Emmanuel/paywithtransactpay_android.git")
                        developerConnection.set("scm:git@github.com:Omamuli-Emmanuel/paywithtransactpay_android.git")
                        url.set("https://github.com/Omamuli-Emmanuel/paywithtransactpay_android")
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

    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Add this line to include your library dependency
    implementation("com.github.Omamuli-Emmanuel:paywithtransactpay_android:0.0.1")
}
