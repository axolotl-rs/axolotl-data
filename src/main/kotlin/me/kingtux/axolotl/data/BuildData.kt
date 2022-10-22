package me.kingtux.axolotl.data;

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.gson.Gson
import me.kingtux.axolotl.data.launcher.VersionFile
import me.kingtux.axolotl.data.launcher.VersionManifest
import me.kingtux.axolotl.data.mappings.InvalidMojangMapMappingVisitor
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
import java.nio.file.Path
import kotlin.io.path.reader


class BuildData : CliktCommand() {
    private val version: String by argument(help = "Minecraft Version to Download")
    private val output: Path by argument().path(canBeFile = false)

    override fun run() {

        if (!output.toFile().exists()) {
            output.toFile().mkdirs()
        }
        println("Downloading $version to $output")
        if (download()) {
            println("Remapping Jar")
            remap()

        }

    }

    private fun remap() {
        val mappings = output.resolve("server-mappings.txt")
        val tree = MemoryMappingTree()
        mappings.reader().use {
            ProGuardReader.read(it, "OB_Minecraft", "Minecraft", tree)
        }
        val mappingsOutput = output.resolve("server-mappings.tiny")

        MappingWriter.create(mappingsOutput, MappingFormat.TINY_2).use { writer ->
            tree.accept(MappingSourceNsSwitch(InvalidMojangMapMappingVisitor(writer), "Minecraft"))
        }
        val server = output.resolve("server.jar")

        val remappedServer = output.resolve("server-remapped.jar")


        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappingsOutput, "OB_Minecraft", "Minecraft"))
            .rebuildSourceFilenames(true).build()


        try {
            OutputConsumerPath.Builder(remappedServer).build().use { outputConsumer ->
                outputConsumer.addNonClassFiles(server, NonClassCopyMode.UNCHANGED, remapper)
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
                        if (downloadFile(response, "server.jar")) return false
                    }
                }
                Request.Builder().url(serverMappings.url).build().let { request ->
                    client.newCall(request).execute().use { response ->
                        if (downloadFile(response, "server-mappings.txt")) return false
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
            echo("No Body")
            return true
        }
        val file = output.resolve(name)
        val toFile = file.toFile()
        toFile.writeBytes(body.bytes())
        return false
    }
}

fun main(args: Array<String>) = BuildData().main(args)