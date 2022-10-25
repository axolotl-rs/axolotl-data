import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"

}

group = "dev.kingtux"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

}
dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("org.reflections:reflections:0.10.2")


    testImplementation(files("../data/server-remapped.jar"))
    testImplementation(fileTree("../data/META-INF/libraries"))

    compileOnly(files("../data/server-remapped.jar"))
    compileOnly(fileTree("../data/META-INF/libraries"))

}
tasks {
    build {
        dependsOn(shadowJar)
    }
    jar {
        archiveFileName.set("dataTool.jarinjar")
    }
    shadowJar {
        archiveFileName.set("datatool-cli.jar")
        manifest {
            attributes(mapOf("Main-Class" to "dev.kingtux.axolotl.data.build.BuildData"))
        }
    }
}
sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}