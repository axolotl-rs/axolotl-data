package dev.kingtux.axolotl.data.build.block

import com.google.gson.Gson
import dev.kingtux.axolotl.data.build.Tag
import dev.kingtux.axolotl.data.build.TagHandler
import net.minecraft.world.level.block.SignBlock

class Handlers : TagHandler<SignBlock> {
    override fun handle(instance: SignBlock): Tag {
        val tags = mapOf(
            "woodType" to Gson().toJsonTree(instance.type().name())
        )
        return Tag("SignBlock", tags)
    }
}