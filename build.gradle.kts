import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager.Companion.host

println(host)
plugins {
    kotlin("multiplatform") version "1.9.20"
    id("de.undercouch.download") version "5.5.0"
}

group = "org.scijava"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val jaunch by creating
            }
        }
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.withType<Executable>().configureEach {
            runTaskProvider?.configure {
                environment("JAVA_HOME", System.getProperty("java.home"))
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:3.6.0")
            }
        }
        val nativeTest by getting
    }

    println(providers.environmentVariable("JAVA_HOME").get())
}

tasks {

    val downloadFiji by registering(Download::class) {
        src("https://downloads.imagej.net/fiji/latest/fiji-linux64.zip")
        dest(layout.buildDirectory.file("fiji.zip").get().asFile)
    }

    val downloadAndUnzipFiji by registering(Copy::class) {
        dependsOn(downloadFiji)
        from(zipTree(downloadFiji))
        into(layout.buildDirectory)
    }
}

