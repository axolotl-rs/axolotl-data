package dev.kingtux.axolotl.data.build.item

import com.google.common.collect.ImmutableListMultimap
import dev.kingtux.axolotl.data.build.Tag
import dev.kingtux.axolotl.data.build.TagHandler

import net.minecraft.core.Registry
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.*
import net.minecraft.world.item.ArmorItem as MinecraftWorldItemArmorItem
import net.minecraft.world.item.DiggerItem as MinecraftDiggerItem
import net.minecraft.world.item.Item as MinecraftItem

enum class CreativeModeTab {
    BUILDING_BLOCKS, DECORATIONS, REDSTONE, TRANSPORTATION, MISC, FOOD, TOOLS, COMBAT, BREWING, MATERIALS, HOTBAR, INVENTORY;

    companion object {
        fun fromMinecraftType(tab: net.minecraft.world.item.CreativeModeTab): CreativeModeTab {
            return when (tab) {
                net.minecraft.world.item.CreativeModeTab.TAB_BREWING -> {
                    BREWING
                }

                net.minecraft.world.item.CreativeModeTab.TAB_BUILDING_BLOCKS -> {
                    BUILDING_BLOCKS
                }

                net.minecraft.world.item.CreativeModeTab.TAB_COMBAT -> {
                    COMBAT
                }

                net.minecraft.world.item.CreativeModeTab.TAB_DECORATIONS -> {
                    DECORATIONS
                }

                net.minecraft.world.item.CreativeModeTab.TAB_FOOD -> {
                    FOOD
                }

                net.minecraft.world.item.CreativeModeTab.TAB_HOTBAR -> {
                    HOTBAR
                }

                net.minecraft.world.item.CreativeModeTab.TAB_INVENTORY -> {
                    INVENTORY
                }

                net.minecraft.world.item.CreativeModeTab.TAB_MATERIALS -> {
                    MATERIALS
                }

                net.minecraft.world.item.CreativeModeTab.TAB_MISC -> {
                    MISC
                }

                net.minecraft.world.item.CreativeModeTab.TAB_REDSTONE -> {
                    REDSTONE
                }

                net.minecraft.world.item.CreativeModeTab.TAB_SEARCH -> {
                    INVENTORY
                }

                net.minecraft.world.item.CreativeModeTab.TAB_TOOLS -> {
                    TOOLS
                }

                net.minecraft.world.item.CreativeModeTab.TAB_TRANSPORTATION -> {
                    TRANSPORTATION
                }

                else -> {
                    // Just in case
                    INVENTORY
                }
            }
        }
    }
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
                            Registry.ATTRIBUTE.getKey(modifier.key as Attribute)?.let {
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
        for (item in Registry.ITEM) {
            val id = Registry.ITEM.getId(item)
            val name = Registry.ITEM.getKey(item).path
            val attributes = getDefaultAttributes(item.javaClass, item)
            val creativeTab =
                if (item.itemCategory == null) null else CreativeModeTab.fromMinecraftType(item.itemCategory!!)
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