package dev.kingtux.axolotl.data.build

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material as MinecraftMaterial

data class BlockProperties(
    val material: String,
    val hasCollision: Boolean,
    val soundType: String,
    val defaultLightEmission: Int,
    val explosionResistance: Float,
    val destroyTime: Float,
    val requiresCorrectToolForDrops: Boolean,
    val isRandomlyTicking: Boolean,
    val friction: Float,
    val speedFactor: Float,
    val jumpFactor: Float,
    val canOcclude: Boolean,
    val isAir: Boolean,
    val drops: String
    // TODO: Add more properties
) {


    companion object {
        // Builds the properties from BlockBehaviour.Properties
        fun buildProperties(block: Block, blockExporter: BlockExporter): BlockProperties {
            val propertiesField = BlockBehaviour::class.java.getDeclaredField("properties");
            propertiesField.isAccessible = true;
            val properties = propertiesField.get(block) as BlockBehaviour.Properties;
            val propertiesClass = properties.javaClass;
            val material = propertiesClass.getDeclaredField("material").let { field ->
                field.isAccessible = true;
                val value = field.get(properties) as MinecraftMaterial;
                blockExporter.materials.firstOrNull { it.equals(value) }?.name ?: "UNKNOWN"
            };

            val soundType = propertiesClass.getDeclaredField("soundType").let { field ->
                field.isAccessible = true;

                val value = field.get(properties) as net.minecraft.world.level.block.SoundType;
                blockExporter.soundTypes.firstOrNull { it.equals(value) }?.name ?: "UNKNOWN"
            };


            return BlockProperties(
                material,
                propertiesClass.readField(properties, "hasCollision"),
                soundType,
                0, // TODO: Add light emission
                propertiesClass.readField(properties, "explosionResistance"),
                propertiesClass.readField(properties, "destroyTime"),
                propertiesClass.readField(properties, "requiresCorrectToolForDrops"),
                propertiesClass.readField(properties, "isRandomlyTicking"),
                propertiesClass.readField(properties, "friction"),
                propertiesClass.readField(properties, "speedFactor"),
                propertiesClass.readField(properties, "jumpFactor"),
                propertiesClass.readField(properties, "canOcclude"),
                propertiesClass.readField(properties, "isAir"),
                block.lootTable.path
            )

        }
    }
}

private fun <T, R> Class<T>.readField(instance: T, name: String): R {
    return this.getDeclaredField(name).run {
        isAccessible = true
        get(instance) as R
    }
}

open class Block(val id: Int, val name: String, val properties: BlockProperties)


class BlockExporter(internal val materials: List<Material>, internal val soundTypes: List<SoundType>) {

    fun run(): List<JsonElement> {
        val items = mutableListOf<JsonElement>()
        val gson = Gson()
        for (item in Registry.BLOCK) {
            val id = Registry.BLOCK.getId(item)
            val name = Registry.BLOCK.getKey(item).path
            val properties = BlockProperties.buildProperties(item, this)

            items.add(gson.toJsonTree(Block(id, name, properties)))
        }
        return items

    }
}