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
        val items = ItemExport().run()
        val gson = GsonBuilder().setPrettyPrinting().create()
        val itemsFile = path.resolve("items.json").toFile()
        if (!itemsFile.exists()) {
            itemsFile.createNewFile()
        }
        val writer = itemsFile.writer()
        gson.toJson(items, writer)
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