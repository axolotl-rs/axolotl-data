package dev.kingtux.axolotl.data.build

import net.minecraft.world.level.material.PushReaction
import java.lang.reflect.Modifier
import net.minecraft.world.level.material.Material as MinecraftMaterial

data class Material(
    val name: String,
    val color: Int,
    val isLiquid: Boolean,
    val pushReaction: PushReaction,
    val blockMotion: Boolean,
    val flammable: Boolean,
    val liquid: Boolean,
    val solidBlocking: Boolean,
    val replaceable: Boolean,
    val solid: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is MinecraftMaterial -> {
                this.color == other.color.id && this.isLiquid == other.isLiquid && this.pushReaction == other.pushReaction && this.blockMotion == other.blocksMotion() && this.flammable == other.isFlammable && this.liquid == other.isLiquid && this.solidBlocking == other.isSolidBlocking && this.replaceable == other.isReplaceable && this.solid == other.isSolid
            }

            is Material -> {
                this.color == other.color && this.isLiquid == other.isLiquid && this.pushReaction == other.pushReaction && this.blockMotion == other.blockMotion && this.flammable == other.flammable && this.liquid == other.liquid && this.solidBlocking == other.solidBlocking && this.replaceable == other.replaceable && this.solid == other.solid
            }

            else -> {
                false
            }
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + color
        result = 31 * result + isLiquid.hashCode()
        result = 31 * result + pushReaction.hashCode()
        result = 31 * result + blockMotion.hashCode()
        result = 31 * result + flammable.hashCode()
        result = 31 * result + liquid.hashCode()
        result = 31 * result + solidBlocking.hashCode()
        result = 31 * result + replaceable.hashCode()
        result = 31 * result + solid.hashCode()
        return result
    }

    companion object {


        /**
         *Builds the Materials from the Minecraft Material class
         *By looping through fields and grabbing the Public, Static, and Final fields
         */
        fun buildMaterials(): List<Material> {
            return MinecraftMaterial::class.java.declaredFields.filter {
                Modifier.isFinal(it.modifiers) && Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)
            }.map {
                (it.get(null) as MinecraftMaterial).let { material ->
                    Material(
                        it.name,
                        material.color.id,
                        material.isLiquid,
                        material.pushReaction,
                        material.blocksMotion(),
                        material.isFlammable,
                        material.isLiquid,
                        material.isSolidBlocking,
                        material.isReplaceable,
                        material.isSolid,
                    )
                }
            }
        }
    }
}
