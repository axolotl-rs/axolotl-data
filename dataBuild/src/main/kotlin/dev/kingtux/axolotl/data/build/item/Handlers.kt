package dev.kingtux.axolotl.data.build.item

import com.google.gson.Gson
import dev.kingtux.axolotl.data.build.GenericTag
import dev.kingtux.axolotl.data.build.Tag
import dev.kingtux.axolotl.data.build.TagHandler
import net.minecraft.core.Registry
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.*

class BlockItemTagHandler() : TagHandler<BlockItem> {
    override fun handle(instance: BlockItem): Tag {
        return GenericTag(
            "BlockItem", mapOf(
                "block" to Gson().toJsonTree(Registry.BLOCK.getKey(instance.block).path)
            )
        )
    }
}

class DiggerItemTagHandler() : TagHandler<DiggerItem> {
    override fun handle(instance: DiggerItem): Tag {
        val toolType = when (instance) {
            is AxeItem -> "Axe"
            is HoeItem -> "Hoe"
            is ShovelItem -> "Shovel"
            is PickaxeItem -> "Pickaxe"
            else -> "Unknown"
        }
        val tier = instance.tier as Tiers
        val blockTag = DiggerItem::class.java.getDeclaredField("blocks").let { field ->
            field.isAccessible = true
            field.get(instance).let {
                if (it is TagKey<*>) {
                    it.location().path
                } else {
                    null
                }
            }
        }
        val gson = Gson()
        return GenericTag(
            "ToolItem", mapOf(
                "toolType" to gson.toJsonTree(toolType),
                "tier" to gson.toJsonTree(tier.name),
                "blockTag" to gson.toJsonTree(blockTag)
            )
        )
    }

}

class TieredItemHandler() : TagHandler<TieredItem> {
    override fun handle(instance: TieredItem): Tag {
        val tier = instance.tier as Tiers
        return GenericTag(
            "TieredItem", mapOf(
                "tier" to Gson().toJsonTree(tier.name)
            )
        )
    }
}

class ArmorItemTagHandler() : TagHandler<ArmorItem> {


    override fun handle(instance: ArmorItem): Tag {
        val equipmentSlot = when (instance.slot) {
            EquipmentSlot.HEAD -> "head"
            EquipmentSlot.CHEST -> "chest"
            EquipmentSlot.LEGS -> "legs"
            EquipmentSlot.FEET -> "feet"
            else -> return GenericTag("ArmorItem", mapOf())
        }
        val material = (instance.material as ArmorMaterials).name
        return GenericTag(
            "ArmorItem", mapOf(
                "equipmentSlot" to Gson().toJsonTree(equipmentSlot),
                "material" to Gson().toJsonTree(material)
            )
        )
    }
}