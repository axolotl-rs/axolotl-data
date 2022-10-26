package dev.kingtux.axolotl.data.build

import net.minecraft.core.Registry
import java.lang.reflect.Modifier
import net.minecraft.world.level.block.SoundType as MinecraftSoundType

data class SoundType(
    val name: String,
    val volume: Float,
    val pitch: Float,
    val breakSound: String,
    val stepSound: String,
    val placeSound: String,
    val hitSound: String,
    val fallSound: String
) {
    override fun equals(other: Any?): Boolean {
        if (other is MinecraftSoundType) {
            return this.volume == other.volume && this.pitch == other.pitch && this.breakSound == other.breakSound.location.path && this.stepSound == other.stepSound.location.path && this.placeSound == other.placeSound.location.path && this.hitSound == other.hitSound.location.path && this.fallSound == other.fallSound.location.path
        } else {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SoundType

            if (volume != other.volume) return false
            if (pitch != other.pitch) return false
            if (breakSound != other.breakSound) return false
            if (stepSound != other.stepSound) return false
            if (placeSound != other.placeSound) return false
            if (hitSound != other.hitSound) return false
            if (fallSound != other.fallSound) return false

            return true
        }
    }

    override fun hashCode(): Int {
        var result = volume.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + breakSound.hashCode()
        result = 31 * result + stepSound.hashCode()
        result = 31 * result + placeSound.hashCode()
        result = 31 * result + hitSound.hashCode()
        result = 31 * result + fallSound.hashCode()
        return result
    }

    companion object {
        /**
         * Builds the SoundTypes from the Minecraft SoundType class
         * By looping through fields and grabbing the Public, Static, and Final fields
         */
        fun buildSoundTypes(): List<SoundType> {
            return MinecraftSoundType::class.java.declaredFields.filter {
                Modifier.isFinal(it.modifiers) && Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)
            }.map {
                val soundType = it.get(null) as MinecraftSoundType
                SoundType(
                    it.name,
                    soundType.volume,
                    soundType.pitch,
                    soundType.breakSound.location.path,
                    soundType.stepSound.location.path,
                    soundType.placeSound.location.path,
                    soundType.hitSound.location.path,
                    soundType.fallSound.location.path
                )
            }
        }
    }
}
