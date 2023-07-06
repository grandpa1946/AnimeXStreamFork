package net.xblacky.animexstream.utils.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import net.xblacky.animexstream.utils.constants.C

public open class AnimeMetaModel(
    @PrimaryKey
    var ID: Int = 0,
    var typeValue: Int? = null,
    var imageUrl: String = "",
    var categoryUrl: String? = null,
    var episodeUrl: String? = null,
    var title: String = "",
    var episodeNumber: String? = null,
    var genreList: RealmList<GenreModel>? = null,
    var timestamp: Long = System.currentTimeMillis(),
    var insertionOrder: Int = -1,
    var releasedDate: String? = null
) : RealmObject() {
    fun toDisplayModel() =
        AnimeDisplayModel(
            id = ID.toString(),
            title = title,
            subtitle = getSubTitle() ?: "",
            imageUrl = imageUrl,
            categoryUrl = categoryUrl
        )

    private fun getSubTitle() = if (typeValue == C.TYPE_MOVIE || typeValue == C.TYPE_NEW_SEASON) releasedDate else episodeNumber
}
