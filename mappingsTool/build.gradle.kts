import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.0"

}

group = "dev.kingtux"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":common"))
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("com.github.FabricMC:tiny-remapper:d14e8f9980")
    implementation("com.github.FabricMc:mapping-io:597f0722d6")
}
tasks {
    jar {
        if (project.hasProperty("jarinjar")) {
            from(project(":dataBuild").tasks.jar.get().archiveFile)
        }

        manifest {
            attributes(mapOf("Main-Class" to "dev.kingtux.axolotl.data.mapping.MappingsToolKt"))
        }
    }
    shadowJar {
        if (project.hasProperty("jarinjar")) {
            from(project(":dataBuild").tasks.jar.get().archiveFile)
        }

        manifest {
            manifest {
                attributes(mapOf("Main-Class" to "dev.kingtux.axolotl.data.mapping.MappingsToolKt"))
            }
        }
    }
    build {
        dependsOn(shadowJar)
    }
}
sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
        }
        resources {

        }
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

