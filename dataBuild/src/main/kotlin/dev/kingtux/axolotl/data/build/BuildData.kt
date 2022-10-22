package dev.kingtux.axolotl.data.build

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class BuildData : CliktCommand() {
    private val output: Path by argument().path(canBeFile = false)
    override fun run() {
        DataBuilder().start(output)
    }

}

fun main(args: Array<String>) = BuildData().main(args)