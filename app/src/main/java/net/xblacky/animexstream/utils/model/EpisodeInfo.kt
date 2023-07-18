package net.xblacky.animexstream.utils.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import net.xblacky.animexstream.utils.extractors.VideoExtractor

open class EpisodeInfo(
    @PrimaryKey
    var episodeUrl: String = "",
    var vidCdnUrl: String = "",
    var nextEpisodeUrl: String? = null,
    var previousEpisodeUrl: String? = null,
    var videoExtractors: List<VideoExtractor?>? = null
) : RealmObject()