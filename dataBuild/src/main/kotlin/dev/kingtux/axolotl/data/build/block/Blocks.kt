package dev.kingtux.axolotl.data.build.block

import com.google.gson.Gson
import com.google.gson.JsonElement
import dev.kingtux.axolotl.data.build.Material
import dev.kingtux.axolotl.data.build.SoundType
import dev.kingtux.axolotl.data.build.Tag
import dev.kingtux.axolotl.data.build.TagHandler
import net.minecraft.core.Registry
import net.minecraft.world.level.block.SignBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block as MinecraftBlock
import net.minecraft.world.level.material.Material as MinecraftMaterial


/**
 * Block Properties based on BlockBehaviour.Properties
 */
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
    // The loot table id
    val drops: String,
    // TODO: Add more properties
) {


    companion object {
        // Builds the properties from BlockBehaviour.Properties
        fun buildProperties(block: MinecraftBlock, blockExporter: BlockExporter): BlockProperties {
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

/**
 * The Block Object
 */
class Block(
    val id: Int, val name: String,
    val properties: BlockProperties,
    val tags: List<Tag> = listOf(),
)


class BlockExporter(
    internal val materials: List<Material>,
    internal val soundTypes: List<SoundType>,
    private val tagHandlers: Map<Class<*>, TagHandler<Any>> = mapOf(
        SignBlock::class.java to (Handlers() as TagHandler<Any>)
    )
) {
    /**
     * Generate Block Tag
     */
    private fun buildTag(clazz: Class<*>, instance: MinecraftBlock): Tag {
        val getValue = tagHandlers[clazz]
        return getValue?.handle(instance) ?: Tag(clazz.simpleName)
    }

    /**
     * Loops through block interfaces and parent classes to generate a list of "tags"
     */
    private fun buildTags(instance: MinecraftBlock, blockClass: Class<*>, tags: MutableList<Tag>) {
        if (blockClass == MinecraftBlock::class.java) return;

        tags.add(buildTag(blockClass, instance))

        blockClass.interfaces.forEach {
            tags.add(buildTag(it, instance))
        }
        buildTags(instance, blockClass.superclass, tags)
    }

    fun run(): List<JsonElement> {
        val items = mutableListOf<JsonElement>()
        val gson = Gson()
        for (item in Registry.BLOCK) {
            val id = Registry.BLOCK.getId(item)
            val name = Registry.BLOCK.getKey(item).path
            val properties = BlockProperties.buildProperties(item, this)
            val tags = mutableListOf<Tag>()
            buildTags(item, item.javaClass, tags)
            items.add(gson.toJsonTree(Block(id, name, properties, tags)))
        }
        return items

    }
}