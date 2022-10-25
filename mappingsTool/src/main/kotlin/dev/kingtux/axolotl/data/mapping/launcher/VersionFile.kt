package dev.kingtux.axolotl.data.mapping.launcher

import com.google.gson.annotations.SerializedName


data class VersionFile(
    val downloads: LauncherDownloads,
)


data class LauncherDownloads(

    val client: Download,
    @SerializedName(value = "client_mappings") val clientMappings: Download,
    val server: Download,
    @SerializedName(value = "server_mappings") val serverMappings: Download
)

data class Download(
    val sha1: String, val size: Long, val url: String, val path: String? = null
)
