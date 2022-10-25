package dev.kingtux.axolotl.data.mapping.launcher

data class VersionManifest(
    val latest: Latest, val versions: List<Version>
)

data class Latest(
    val release: String, val snapshot: String
)

data class Version(
    val id: String, val type: Type, val url: String, val time: String, val releaseTime: String
)

enum class Type(val value: String) {
    OldAlpha("old_alpha"), OldBeta("old_beta"), Release("release"), Snapshot("snapshot");

    companion object {
        public fun fromValue(value: String): Type = when (value) {
            "old_alpha" -> OldAlpha
            "old_beta" -> OldBeta
            "release" -> Release
            "snapshot" -> Snapshot
            else -> throw IllegalArgumentException()
        }
    }
}
