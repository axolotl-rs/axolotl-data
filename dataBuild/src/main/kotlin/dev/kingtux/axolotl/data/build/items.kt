package dev.kingtux.axolotl.data.build

import com.google.common.collect.ImmutableListMultimap
import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.core.Registry
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.*
import net.minecraft.world.level.block.Block

enum class ItemType {
    BLOCK, ITEM
}

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

enum class ToolType {
    AXE, HOE, PICKAXE, SHOVEL
}

data class DefaultAttributeValue(
    val attribute: String,
    val value: Double
)

open class Item(
    val name: String,
    val type: ItemType,
    val id: Int,
    val maxStackSize: Int = 64,
    val creativeTab: CreativeModeTab? = null,
    val attributes: List<DefaultAttributeValue> = listOf(),
);

open class TieredItem(
    name: String,
    id: Int,
    maxStackSize: Int = 64,
    creativeTab: CreativeModeTab? = null,
    attributes: List<DefaultAttributeValue> = listOf(),
    val tier: String,
) : Item(name, ItemType.ITEM, id, maxStackSize, creativeTab = creativeTab, attributes = attributes)

open class ToolItem(
    name: String,
    id: Int,
    maxStackSize: Int = 64,
    tier: String,
    creativeTab: CreativeModeTab? = null,
    attributes: List<DefaultAttributeValue> = listOf(),

    val toolType: ToolType,
    val blockTag: String? = null,

    ) : TieredItem(name, id, maxStackSize, creativeTab, attributes, tier)

open class ArmorItem(
    name: String,
    id: Int,
    creativeTab: CreativeModeTab? = null,
    attributes: List<DefaultAttributeValue> = listOf(),

    val armorSlot: String,
    val material: String,

    ) : Item(name, ItemType.ITEM, id, 1, creativeTab = creativeTab, attributes = attributes)


class ItemExport {
    private fun getDefaultAttributes(
        item: Class<*>,
        itemValue: net.minecraft.world.item.Item
    ): List<DefaultAttributeValue> {

        val attributes = try {
            val attributes = mutableListOf<DefaultAttributeValue>()

            val defaultModifiers = item.getDeclaredField("defaultModifiers")
            defaultModifiers.isAccessible = true
            val modifiers = defaultModifiers.get(itemValue) as ImmutableListMultimap<Attribute, AttributeModifier>;
            for (modifier in modifiers.entries()) {
                Registry.ATTRIBUTE.getKey(modifier.key)?.let {
                    attributes.add(DefaultAttributeValue(it.path, modifier.value.amount))
                }
            }
            attributes
        } catch (e: NoSuchFieldException) {
            mutableListOf<DefaultAttributeValue>()
        };
        attributes += item.superclass.let {
            if (it != null) {
                getDefaultAttributes(it, itemValue)
            } else {
                listOf()
            }
        }
        return attributes
    }

    fun run(): List<JsonElement> {
        val items = mutableListOf<JsonElement>()
        for (item in Registry.ITEM) {
            val id = Registry.ITEM.getId(item)
            val name = Registry.ITEM.getKey(item).path
            val attributes = getDefaultAttributes(item.javaClass, item);
            val creativeTab =
                if (item.itemCategory == null) null else CreativeModeTab.fromMinecraftType(item.itemCategory!!)
            val gson = Gson()
            if (item is net.minecraft.world.item.TieredItem) {
                if (item is DiggerItem) {
                    items.add(
                        gson.toJsonTree(
                            this.tool(
                                name,
                                id,
                                item,
                                creativeTab,
                                attributes,
                            )
                        )
                    )
                } else {
                    items.add(
                        gson.toJsonTree(
                            TieredItem(
                                name,
                                id,
                                item.maxStackSize,
                                creativeTab,
                                attributes,
                                (item.tier as Tiers).name,
                            )
                        )
                    )
                }

            } else if (item is net.minecraft.world.item.ArmorItem) {
                items.add(
                    gson.toJsonTree(
                        ArmorItem(
                            name,
                            id,
                            creativeTab,
                            attributes,
                            item.slot.name,
                            (item.material as ArmorMaterials).name,
                        )
                    )
                )
            } else{
                val type = if (item is BlockItem) ItemType.BLOCK else ItemType.ITEM
                val maxStackSize = item.maxStackSize
                items.add(
                    gson.toJsonTree(
                        Item(
                            name,
                            type,
                            id,
                            maxStackSize,
                            creativeTab,
                            attributes
                        )
                    )
                )
            }
        }
        return items

    }

    private fun armor(
        name: String,
        id: Int,
        item: net.minecraft.world.item.ArmorItem,
        creativeTab: CreativeModeTab?,
        attributes: List<DefaultAttributeValue>
    ): ArmorItem? {
        val equipmentSlot = when (item.slot) {
            EquipmentSlot.HEAD -> "head"
            EquipmentSlot.CHEST -> "chest"
            EquipmentSlot.LEGS -> "legs"
            EquipmentSlot.FEET -> "feet"
            else -> return null;
        }
        val material = (item.material as ArmorMaterials).name


        return ArmorItem(
            name,
            id,
            creativeTab,
            attributes,
            equipmentSlot,
            material,
        )
    }

    private fun tool(
        name: String,
        id: Int,
        item: DiggerItem,
        creativeTab: CreativeModeTab?,
        attributes: List<DefaultAttributeValue>
    ): ToolItem? {
        val toolType = when (item) {
            is AxeItem -> ToolType.AXE
            is HoeItem -> ToolType.HOE
            is ShovelItem -> ToolType.SHOVEL
            is PickaxeItem -> ToolType.PICKAXE
            else -> {
                println("Unknown tool type for $name")
                return null;
            }
        }
        val tier = item.tier as Tiers;
        val blockTag = DiggerItem::class.java.getDeclaredField("blocks").let {
            it.isAccessible = true
            it.get(item) as TagKey<Block>
        };
        return ToolItem(
            name,
            id,
            item.maxStackSize,
            tier.name,
            creativeTab,
            attributes,
            toolType,
            blockTag.location.path
        )
    }
}