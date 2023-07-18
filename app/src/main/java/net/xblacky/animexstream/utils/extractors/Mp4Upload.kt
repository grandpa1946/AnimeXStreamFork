package net.xblacky.animexstream.utils.extractors

import net.xblacky.animexstream.utils.getSize

class Mp4Upload(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(response: String): VideoContainer {
        println("UHhh Mp4")
        val link = client.get(server.embed.url).document
            .select("script").html()
            .substringAfter("src: \"").substringBefore("\"")
        return VideoContainer(listOf(Video(null, VideoType.CONTAINER, link, getSize(link))))
    }
}