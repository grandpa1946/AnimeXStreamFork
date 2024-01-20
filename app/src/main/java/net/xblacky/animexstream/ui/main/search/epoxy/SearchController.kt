package net.xblacky.animexstream.ui.main.search.epoxy

import android.view.View
import com.airbnb.epoxy.Typed2EpoxyController
import net.xblacky.animexstream.utils.epoxy.AnimeCommonModel_
import net.xblacky.animexstream.utils.epoxy.LoadingModel_
import net.xblacky.animexstream.utils.model.AnimeDisplayModel
import net.xblacky.animexstream.utils.model.AnimeMetaModel

class SearchController(var adapterCallbacks: EpoxySearchAdapterCallbacks) :
    Typed2EpoxyController<ArrayList<AnimeMetaModel>, Boolean>() {


    override fun buildModels(data: ArrayList<AnimeMetaModel>?, isLoading: Boolean) {
        data?.forEach { animeMetaModel ->
            AnimeCommonModel_()
                .id(animeMetaModel.ID)
                .animeDisplayModel(animeMetaModel.toDisplayModel())
                .spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount / totalSpanCount }
                .clickListener { model, holder, _, _ ->
                    adapterCallbacks.animeTitleClick(
                        model = model.animeDisplayModel(),
                        sharedImage = holder.animeImageView,
                        sharedTitle = holder.animeTitle
                    )
                }
                .addTo(this)
        }
        if (!data.isNullOrEmpty()) {
            LoadingModel_()
                .id("loading")
                .spanSizeOverride { totalSpanCount, _, _ ->
                    totalSpanCount
                }
                .addIf(isLoading, this)
        }
    }


    interface EpoxySearchAdapterCallbacks {
        fun animeTitleClick(model: AnimeDisplayModel, sharedTitle: View, sharedImage: View)
    }

}