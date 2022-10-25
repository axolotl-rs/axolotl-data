package dev.kingtux.axolotl.data.mapping.mappings


import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor
// https://github.com/OrionCraftMC/LightClientGradle/blob/main/src/main/kotlin/io/github/nickacpt/lightcraft/gradle/providers/mappings/InvalidMojangMapMappingVisitor.kt
class InvalidMojangMapMappingVisitor(next: MappingVisitor?) : ForwardingMappingVisitor(next) {
    private var conflictCounter = 0

    override fun visitDstName(targetKind: MappedElementKind, namespace: Int, name: String?) {
        var finalName = name

        if (name?.startsWith("lambda$") == true) {
            finalName = "${name}_${++conflictCounter}"
        }

        super.visitDstName(targetKind, namespace, finalName)
    }
}