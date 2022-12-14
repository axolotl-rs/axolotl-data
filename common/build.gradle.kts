
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
}

group = "dev.kingtux"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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

