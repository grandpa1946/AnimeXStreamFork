package net.xblacky.animexstream.ui.main.home.epoxy

import android.view.View
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.Carousel.setDefaultGlobalSnapHelperFactory
import com.airbnb.epoxy.CarouselModel_
import com.airbnb.epoxy.TypedEpoxyController
import net.xblacky.animexstream.utils.epoxy.AnimeCommonModel_
import net.xblacky.animexstream.utils.model.AnimeDisplayModel
import net.xblacky.animexstream.utils.model.HomeScreenModel


class HomeController(private var adapterCallbacks: EpoxyAdapterCallbacks) :
    TypedEpoxyController<ArrayList<HomeScreenModel>>() {


    override fun buildModels(data: ArrayList<HomeScreenModel>) {
        data.forEach { homeScreenModel ->

            AnimeMiniHeaderModel_()
                .id(homeScreenModel.typeValue)
                .typeName(homeScreenModel.type)
                .addIf(!homeScreenModel.animeList.isNullOrEmpty(), this)


            val animeModelList: ArrayList<AnimeCommonModel_> = ArrayList()
            homeScreenModel.animeList?.forEach {
                val animeMetaModel = it

                animeModelList.add(
                    AnimeCommonModel_()
                        .id(animeMetaModel.ID)
                        .clickListener { model, holder, _, _ ->
                            adapterCallbacks.animeTitleClick(
                                model = model.animeDisplayModel(),
                                sharedTitle = holder.animeTitle,
                                sharedImage = holder.animeImageView
                            )
                        }
                        .animeDisplayModel(animeMetaModel.toDisplayModel())
                )
            }
            setDefaultGlobalSnapHelperFactory(null)

            CarouselModel_()
                .id(homeScreenModel.hashCode())
                .models(animeModelList)
                .padding(Carousel.Padding.dp(20, 0, 20, 0, 20))
                .addTo(this)
        }

    }

    interface EpoxyAdapterCallbacks {
        fun animeTitleClick(model: AnimeDisplayModel, sharedTitle: View, sharedImage: View)
    }
}
