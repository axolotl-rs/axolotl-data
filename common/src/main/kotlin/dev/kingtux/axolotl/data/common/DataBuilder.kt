package dev.kingtux.axolotl.data.common

import java.nio.file.Path

interface DataBuilder {
    fun start(path: Path);
}