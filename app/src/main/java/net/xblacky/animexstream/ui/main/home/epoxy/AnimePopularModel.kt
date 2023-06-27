package net.xblacky.animexstream.ui.main.home.epoxy

import android.view.View
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import net.xblacky.animexstream.R
import net.xblacky.animexstream.databinding.RecyclerAnimePopularBinding
import net.xblacky.animexstream.utils.Tags.GenreTags
import net.xblacky.animexstream.utils.model.AnimeMetaModel

@EpoxyModelClass(layout = R.layout.recycler_anime_popular)
abstract class AnimePopularModel : EpoxyModelWithHolder<AnimePopularModel.PopularHolder>() {

    @EpoxyAttribute
    lateinit var animeMetaModel: AnimeMetaModel

    @EpoxyAttribute
    lateinit var clickListener: View.OnClickListener

    override fun bind(holder: PopularHolder) {
        Glide.with(holder.binding.animeImage.context).load(animeMetaModel.imageUrl).centerCrop()
            .into(holder.binding.animeImage)
        holder.binding.animeTitle.text = animeMetaModel.title
        holder.binding.episodeNumber.text = animeMetaModel.episodeNumber

        holder.binding.flowLayout.removeAllViews()

        animeMetaModel.genreList?.forEach {
            holder.binding.flowLayout.addView(
                GenreTags(holder.binding.flowLayout.context).getGenreTag(
                    it.genreName,
                    it.genreUrl
                )
            )
        }
        holder.binding.root.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition =
            holder.binding.root.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeMetaModel.title}_${animeMetaModel.ID}"
        holder.binding.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition =
            holder.binding.root.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeMetaModel.imageUrl}_${animeMetaModel.ID}"
        holder.binding.animeImage.transitionName = animeImageTransition

    }


    class PopularHolder : EpoxyHolder() {
        lateinit var binding: RecyclerAnimePopularBinding

        override fun bindView(itemView: View) {
            binding = RecyclerAnimePopularBinding.bind(itemView)
        }
    }
}