package dev.kingtux.axolotl.data.build.item

import com.google.common.collect.ImmutableListMultimap
import dev.kingtux.axolotl.data.build.Tag
import dev.kingtux.axolotl.data.build.TagHandler

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.*
import net.minecraft.world.item.ArmorItem as MinecraftWorldItemArmorItem
import net.minecraft.world.item.DiggerItem as MinecraftDiggerItem
import net.minecraft.world.item.Item as MinecraftItem

enum class CreativeModeTab {
    BUILDING_BLOCKS, DECORATIONS, REDSTONE, TRANSPORTATION, MISC, FOOD, TOOLS, COMBAT, BREWING, MATERIALS, HOTBAR, INVENTORY;

}



data class DefaultAttributeValue(
    val attribute: String, val value: Double
)

class Item(
    val name: String,
    val id: Int,
    val maxStackSize: Int = 64,
    val creativeTab: CreativeModeTab? = null,
    val attributes: List<DefaultAttributeValue> = listOf(),
    val tags: List<Tag> = listOf(),
);

class ItemExport(
    private val tagHandlers: Map<Class<*>, TagHandler<Any>> = mapOf(
        BlockItem::class.java to BlockItemTagHandler() as TagHandler<Any>,
        MinecraftDiggerItem::class.java to DiggerItemTagHandler() as TagHandler<Any>,
        MinecraftWorldItemArmorItem::class.java to ArmorItemTagHandler() as TagHandler<Any>,
        TieredItem::class.java to TieredItemHandler() as TagHandler<Any>,
    ),
) {
    private fun getDefaultAttributes(
        item: Class<*>, itemValue: MinecraftItem
    ): List<DefaultAttributeValue> {

        val attributes = try {
            val attributes = mutableListOf<DefaultAttributeValue>()

            val defaultModifiers = item.getDeclaredField("defaultModifiers")
            defaultModifiers.isAccessible = true
            defaultModifiers.get(itemValue).let { modifiers ->
                if (modifiers is ImmutableListMultimap<*, *>) {
                    for (modifier in modifiers.entries()) {
                        if (modifier.key is Attribute && modifier.value is AttributeModifier) {
                            BuiltInRegistries.ATTRIBUTE.getKey(modifier.key as Attribute)?.let {
                                attributes.add(
                                    DefaultAttributeValue(
                                        it.path,
                                        (modifier.value as AttributeModifier).amount
                                    )
                                )
                            }
                        }

                    }
                }
            }


            attributes
        } catch (e: NoSuchFieldException) {
            mutableListOf()
        }
        attributes += item.superclass.let {
            if (it != null) {
                getDefaultAttributes(it, itemValue)
            } else {
                listOf()
            }
        }
        return attributes
    }

    private fun buildTag(clazz: Class<*>, instance: MinecraftItem): Tag {
        val getValue = tagHandlers[clazz]
        return getValue?.handle(instance) ?: Tag(clazz.simpleName)
    }

    /**
     * Loops through block interfaces and parent classes to generate a list of "tags"
     */
    private fun buildTags(instance: MinecraftItem, blockClass: Class<*>, tags: MutableList<Tag>) {
        if (blockClass == MinecraftItem::class.java) return;

        tags.add(buildTag(blockClass, instance))

        blockClass.interfaces.forEach {
            tags.add(buildTag(it, instance))
        }
        buildTags(instance, blockClass.superclass, tags)

    }

    fun run(): List<Item> {
        val items = mutableListOf<Item>()
        for (item in BuiltInRegistries.ITEM) {
            val id = BuiltInRegistries.ITEM.getId(item)
            val name = BuiltInRegistries.ITEM.getKey(item).path
            val attributes = getDefaultAttributes(item.javaClass, item)
            val creativeTab = null
            val tags = mutableListOf<Tag>()
            this.buildTags(item, item.javaClass, tags)
            val itemData = Item(
                name = name,
                id = id,
                maxStackSize = item.maxStackSize,
                creativeTab = creativeTab,
                attributes = attributes,
                tags = tags
            )
            items.add(itemData)
        }
        return items

    }

}