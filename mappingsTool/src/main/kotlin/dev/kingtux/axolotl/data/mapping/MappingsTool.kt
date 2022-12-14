package dev.kingtux.axolotl.data.mapping;

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.gson.Gson
import dev.kingtux.axolotl.data.common.DataBuilder
import dev.kingtux.axolotl.data.mapping.launcher.VersionFile
import dev.kingtux.axolotl.data.mapping.launcher.VersionManifest
import dev.kingtux.axolotl.data.mapping.mappings.InvalidMojangMapMappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.format.ProGuardReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.reader


class BuildData : CliktCommand() {
    private val version: String by argument(help = "Minecraft Version to Download")
    private val output: Path by argument().path(canBeFile = false)
    override fun run() {

        if (!output.resolve(version).toFile().exists()) {
            output.toFile().mkdirs()
        }
        println("Downloading $version to $output")
        if (download()) {
            println("Downloaded $version to $output")
            extractJar()
            println("Extracted $version to $output")
            remap()
            println("Remapped $version to $output")
            runData()
            println("Ran Data for $version to $output")
        } else {
            println("Failed to download $version to $output")
        }

    }

    private fun runData() {
        val jarInJar = BuildData::class.java.getResource("/dataTool.jarinjar")
        if (jarInJar == null) {
            println("Could not find dataTool.jarinjar")
            return
        }
        val dataToolJar = output.resolve("dataTool.jar")
        if (dataToolJar.toFile().exists()) {
            dataToolJar.toFile().delete()
        }
        Files.copy(jarInJar.openStream(), dataToolJar)
        val dataLoader = mutableListOf<URL>(dataToolJar.toUri().toURL())
        for (file in output.resolve(version).resolve("META-INF/libraries").toFile().walk()) {
            if (file.isFile && file.extension == "jar") {
                dataLoader.add(file.toURI().toURL())
            }
        }
        dataLoader.add(output.resolve(version).resolve("server-remapped.jar").toUri().toURL())
        val classLoader = DataClassLoader(dataLoader.toTypedArray())
        val mainClass = classLoader.loadClass("dev.kingtux.axolotl.data.build.DataBuilder", true)
        val mainMethod = mainClass.getConstructor().newInstance().let {
            if (it is DataBuilder) {
                it
            } else {
                throw IllegalStateException("DataBuilder is not a DataBuilder")
            }
        };
        mainMethod.javaClass.getMethod("start", Path::class.java).invoke(mainMethod, output.resolve(version))
    }

    private fun extractJar() {
        val server = output.resolve(version).resolve("raw.jar")
        val jarFile = JarFile(server.toFile())
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (!entry.name.startsWith("META-INF")) continue
            if (entry.isDirectory) {
                continue
            }
            val file = output.resolve(version).resolve(entry.name)
            if (!file.parent.toFile().exists()) {
                file.parent.toFile().mkdirs()
            }
            if (file.toFile().exists()) {
                return
            }
            jarFile.getInputStream(entry).use { input ->
                file.toFile().outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

    }

    private fun remap() {
        val mappingsOutput = output.resolve(version).resolve("server-mappings.tiny")
        if (!mappingsOutput.toFile().exists()) {
            val mappings = output.resolve(version).resolve("server-mappings.txt")
            val tree = MemoryMappingTree()
            mappings.reader().use {
                ProGuardReader.read(it, "named", "official", tree)
            }
            MappingWriter.create(mappingsOutput, MappingFormat.TINY_2).use { writer ->
                tree.accept(MappingSourceNsSwitch(InvalidMojangMapMappingVisitor(writer), "named"))
            }
        }

        val server = output.resolve(version).resolve("META-INF/versions/$version/server-$version.jar")


        val remappedServer = output.resolve(version).resolve("server-remapped.jar")
        if (remappedServer.toFile().exists()) {
            println("Skipping Remap as it already exists")
            return;
        }

        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappingsOutput, "official", "named"))
            .rebuildSourceFilenames(true).fixPackageAccess(true).ignoreFieldDesc(false).renameInvalidLocals(true)
            .checkPackageAccess(true).build()


        try {
            OutputConsumerPath.Builder(remappedServer).build().use { outputConsumer ->
                outputConsumer.addNonClassFiles(server, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readClassPath(output.resolve(version).resolve("META-INF/libraries"))
                remapper.readInputs(server)
                remapper.apply(outputConsumer)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            remapper.finish()
        }
    }

    private fun download(): Boolean {
        val client = OkHttpClient()
        val gson = Gson();
        val get = Request.Builder().url("https://piston-meta.mojang.com/mc/game/version_manifest.json").build();
        client.newCall(get).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            val versionManifest = gson.fromJson(response.body?.charStream(), VersionManifest::class.java)
            val version = versionManifest.versions.firstOrNull { it.id == this.version }
            if (version == null) {
                echo("Version ${this.version} not found")
                return false
            }
            val versionGet = Request.Builder().url(version.url).build()
            client.newCall(versionGet).execute().use {
                val versionFile = gson.fromJson(it.body?.charStream(), VersionFile::class.java)
                // Download server.jar and server mappings
                val server = versionFile.downloads.server
                val serverMappings = versionFile.downloads.serverMappings
                Request.Builder().url(server.url).build().let { request ->
                    client.newCall(request).execute().use { response ->
                        if (!downloadFile(response, "raw.jar")) return false
                    }
                }
                Request.Builder().url(serverMappings.url).build().let { request ->
                    client.newCall(request).execute().use { response ->
                        if (!downloadFile(response, "server-mappings.txt")) return false
                    }
                }
            }
        }
        return true
    }

    private fun downloadFile(response: Response, name: String): Boolean {
        if (!response.isSuccessful) throw Exception("Unexpected code $response")
        val body = response.body
        if (body == null) {
            println("No Body")
            return false
        }
        val file = output.resolve(version).resolve(name)
        val toFile = file.toFile()
        if (!toFile.exists()) {
            println("Downloading $name")
            toFile.parentFile.mkdirs()
            toFile.createNewFile()
        } else {
            return true;
        }
        toFile.writeBytes(body.bytes())
        return true
    }
}

fun main(args: Array<String>) = BuildData().main(args)