package dev.kingtux.axolotl.data.build.block

import dev.kingtux.axolotl.data.build.Tag
import dev.kingtux.axolotl.data.build.TagHandler
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.SignBlock

class SignTag(
    val woodType: String
) : Tag("SignBlock");
class SignHandler : TagHandler<SignBlock> {
    override fun handle(instance: SignBlock): Tag {
        return SignTag(instance.type().name());
    }
}

class BedTag(
    val bedColor: String
) : Tag("BedBlock");
class BedHandler : TagHandler<BedBlock> {
    override fun handle(instance: BedBlock): Tag {
        return BedTag(instance.color.name);
    }
}