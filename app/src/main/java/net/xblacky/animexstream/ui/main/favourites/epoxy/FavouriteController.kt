package net.xblacky.animexstream.ui.main.favourites.epoxy

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import net.xblacky.animexstream.utils.epoxy.AnimeCommonModel_
import net.xblacky.animexstream.utils.model.AnimeDisplayModel
import net.xblacky.animexstream.utils.model.FavouriteModel

class FavouriteController(private var adapterCallbacks: EpoxySearchAdapterCallbacks) :
    TypedEpoxyController<ArrayList<FavouriteModel>>() {
    override fun buildModels(data: ArrayList<FavouriteModel>?) {
        data?.let { arrayList ->
            arrayList.forEach {

                AnimeCommonModel_()
                    .id(it.animeName)
                    .animeDisplayModel(it.toDisplayModel())
                    .spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount / totalSpanCount }
                    .clickListener { model, holder, _, _ ->
                        adapterCallbacks.animeTitleClick(
                            model = model.animeDisplayModel(),
                            sharedTitle = holder.animeTitle,
                            sharedImage = holder.animeImageView
                        )
                    }
                    .addTo(this)
            }
        }
    }

    interface EpoxySearchAdapterCallbacks {
        fun animeTitleClick(model: AnimeDisplayModel, sharedTitle: View, sharedImage: View)
    }

}

private fun FavouriteModel.toDisplayModel() =
    AnimeDisplayModel(ID, animeName ?: "", releasedDate ?: "", imageUrl ?: "", categoryUrl)
