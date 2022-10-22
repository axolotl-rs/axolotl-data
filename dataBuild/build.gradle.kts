import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "dev.kingtux"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

}
tasks.jar {
    archiveFileName.set("dataBuild.jar")
}
dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("org.reflections:reflections:0.10.2")

    implementation(files("../data/server-remapped.jar"))
    implementation(fileTree("../data/META-INF/libraries"))

}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}