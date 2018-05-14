import java.net.URI

plugins {
    id("com.android.library") version "3.2.0-alpha13"
    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC6-4"
    id("org.jetbrains.kotlin.kapt") version "1.2.41"
    id("com.palantir.jacoco-full-report") version "0.4.0"
    kotlin("android") version "1.2.41"
}

repositories {
    jcenter()
    google()
    maven { url = URI("https://jitpack.io") }
    maven {
        credentials {
            username = "android"
            password = "android2192"
        }
        authentication {
            BasicAuthentication { "basic" }
        }
        url = URI("https://client-ci.vcube.sg:8081/artifactory/blackhole/")
    }
}


jacoco {
    toolVersion = "0.8.1"
    reportsDir = file("$buildDir/reports")
}
//
//Properties properties = new Properties()
//properties.load project.rootProject.file("version.properties").newDataInputStream()
//
android {
    compileSdkVersion(27)

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(27)

        versionCode = 1
        versionName = "1.0.0" // properties.getOrDefault("library.version", "1.0.0")

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }

        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    detekt {
        version = "1.0.0.RC6-4"

        profile("main", Action {
            input = "src/main/kotlin"
            filters = ".*/resources/.*,.*/build/.*"
            config = file("default-detekt-config.yml")
            output = "reports"
            outputName = "detekt-report"
            baseline = "reports/baseline.xml"
        })
    }

    testOptions {
        animationsDisabled = true
        unitTests.apply {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all(KotlinClosure1<Any, Test>({
                (this as Test).also { testTask ->
                    testTask.extensions
                            .getByType(JacocoTaskExtension::class.java)
                            .isIncludeNoLocationClasses = true
                }
            }, this))
        }
        setExecution("ANDROID_TEST_ORCHESTRATOR")
    }
}

tasks {

    val jacocoTestReport by creating(JacocoReport::class) {
        dependsOn("testDebugUnitTest")
        group = "Reporting"
        description = "Generate Jacoco coverage reports for Debug build"

        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }

        // what to exclude from coverage report
        // UI, "noise", generated classes, platform classes, etc.
        val excludesList = setOf(
                "**/R.class",
                "**/R$*.class",
                "**/*\$ViewInjector*.*",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "android/**/*.*",
                "**/*Fragment.*",
                "**/*Activity.*"
        )
        // generated classes
        classDirectories = fileTree("$buildDir/intermediates/classes/debug") {
            setExcludes(excludesList)
        } + fileTree("$buildDir/tmp/kotlin-classes/debug") {
            setExcludes(excludesList)
        }

        // sources
        sourceDirectories = files(setOf(
                android.sourceSets.getByName("main").java.srcDirs,
                File("src/main/kotlin")
        ))
        executionData = files("$buildDir/jacoco/testDebugUnitTest.exec")
    }

    val jarTests by creating(Jar::class) {
        dependsOn("assembleDebugUnitTest")
        classifier = "tests"
        from("$buildDir/intermediates/classes/test/debug")
    }
}

//configurations {
//    unitTestArtifact
//}
//
//artifacts {
//    unitTestArtifact jarTests
//}

dependencies {
    kapt("com.android.databinding:compiler:3.1.2")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.8.9")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
    testImplementation("android.arch.core:core-testing:1.1.1")
    testImplementation("org.powermock:powermock-api-mockito2:1.7.4")
    testImplementation("org.powermock:powermock-module-junit4:1.7.4")
    testImplementation("org.powermock:powermock-module-junit4-rule:1.7.4")
    testImplementation("org.powermock:powermock-module-junit4-rule-agent:1.7.4")
    testImplementation("org.robolectric:robolectric:3.6.1")


    implementation("com.google.dagger:dagger:2.15")
    implementation("com.google.dagger:dagger-android:2.15")
    implementation("com.google.dagger:dagger-android-support:2.15")

    kapt("com.google.dagger:dagger-compiler:2.15")
    kapt("com.google.dagger:dagger-android-processor:2.15")

    implementation("com.vidyo:VidyoClient:4.1.21.7")
    implementation("io.reactivex.rxjava2:rxjava:2.1.12")
    implementation("org.javatuples:javatuples:1.2")
    implementation("com.github.pakoito:RxTuples2:1.0.0")

    implementation("com.android.support:appcompat-v7:27.1.1")
    implementation("android.arch.lifecycle:extensions:1.1.1")
    implementation("com.android.support.constraint:constraint-layout:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.2.41")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.2.41")
}