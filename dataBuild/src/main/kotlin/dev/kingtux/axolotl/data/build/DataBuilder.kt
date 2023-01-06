package dev.kingtux.axolotl.data.build

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.bridge.game.PackType
import dev.kingtux.axolotl.data.build.block.BlockExporter
import dev.kingtux.axolotl.data.build.item.ItemExport
import net.minecraft.SharedConstants
import net.minecraft.WorldVersion
import net.minecraft.server.Bootstrap
import net.minecraft.world.level.storage.DataVersion
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.*

public interface TagHandler<T> {
    fun handle(instance: T): Tag
}

open class Tag(
    val name: String,
)

class GenericTag(
    name: String, val properties: Map<String, JsonElement>
) : Tag(name)

class GenericTagSerializer : JsonSerializer<GenericTag> {
    override fun serialize(tag: GenericTag, p1: Type, context: JsonSerializationContext): JsonElement {
        val map = JsonObject();
        map.addProperty("name", tag.name);
        for ((key, value) in tag.properties) {
            map.add(key, value);
        }
        return map;
    }
}

class DataBuilder : WorldVersion, dev.kingtux.axolotl.data.common.DataBuilder {
    override fun start(path: Path) {
        val pathAsAbsolute = path.toAbsolutePath()

        println("Tricking Minecraft into thinking it's an actual server")
        SharedConstants.setVersion(this)
        SharedConstants.IS_RUNNING_IN_IDE = true
        SharedConstants.CHECK_DATA_FIXER_SCHEMA = false
        Bootstrap.bootStrap()
        println("Done tricking Minecraft")

        println("Starting Data Export to $pathAsAbsolute")
        val materials: List<Material> = Material.buildMaterials()
        val sounds: List<SoundType> = SoundType.buildSoundTypes()

        println("Exporting Materials")
        this.writeFile(pathAsAbsolute.resolve("materials.json"), materials)
        println("Exporting Sounds")
        this.writeFile(pathAsAbsolute.resolve("soundTypes.json"), sounds)
        println("Exporting Blocks")
        this.writeFile(pathAsAbsolute.resolve("blocks.json"), BlockExporter(materials, sounds).run())
        println("Exporting Items")
        this.writeFile(pathAsAbsolute.resolve("items.json"), ItemExport().run())
    }

    private fun writeFile(path: Path, data: List<Any>) {
        val gson = GsonBuilder().setPrettyPrinting().registerTypeAdapter(GenericTag::class.java, GenericTagSerializer())
            .create()
        val file = path.toFile()
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = file.writer()
        gson.toJson(data, writer)
        writer.close()
    }

    override fun getId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    override fun getName(): String {
        return "1.19.3"
    }



    override fun getProtocolVersion(): Int {
        return SharedConstants.getProtocolVersion()
    }

    override fun getPackVersion(p0: PackType?): Int {
        return 10
    }

    override fun getBuildTime(): Date {
        return Date()
    }

    override fun isStable(): Boolean {
        return false
    }

    override fun getDataVersion(): DataVersion {
        return DataVersion(0)
    }


}