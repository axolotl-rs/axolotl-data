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

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("com.github.FabricMC:tiny-remapper:d14e8f9980")
    implementation("com.github.FabricMc:mapping-io:597f0722d6")
    compileOnly(project(":dataBuild"))

}
sourceSets{
    main{
        kotlin{
            srcDir("src/main/kotlin")
        }
        resources{
            srcDir("../dataBuild/build/libs")
        }
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}