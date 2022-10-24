package dev.kingtux.axolotl.data.build

import com.google.gson.GsonBuilder
import net.minecraft.SharedConstants
import net.minecraft.WorldVersion
import net.minecraft.server.Bootstrap
import net.minecraft.world.level.storage.DataVersion
import java.nio.file.Path
import java.util.*

class DataBuilder : WorldVersion {
    public fun start(path: Path) {
        val path = path.toAbsolutePath()

        SharedConstants.setVersion(this)
        SharedConstants.IS_RUNNING_IN_IDE = true
        SharedConstants.CHECK_DATA_FIXER_SCHEMA = false
        Bootstrap.bootStrap();
        println("Starting Data Export to $path")
        val materials: List<Material> = Material.load()
        val sounds: List<SoundType> = SoundType.load()


        this.writeFile(path.resolve("materials.json"), materials)
        this.writeFile(path.resolve("soundTypes.json"), sounds)
        this.writeFile(path.resolve("blocks.json"), BlockExporter(materials, sounds).run())
        this.writeFile(path.resolve("items.json"), ItemExport().run())
    }
    fun writeFile(path: Path, data: List<Any>){
        val gson = GsonBuilder().setPrettyPrinting().create()
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
        return "1.19.2"
    }

    override fun getReleaseTarget(): String {
        return "1.19.2"
    }

    override fun getProtocolVersion(): Int {
        return SharedConstants.getProtocolVersion()
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