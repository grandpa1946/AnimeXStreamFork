package net.xblacky.animexstream.ui.main.home.epoxy

import android.view.View
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import net.xblacky.animexstream.R
import net.xblacky.animexstream.databinding.RecyclerAnimeRecentSubDub2Binding
import net.xblacky.animexstream.utils.model.AnimeMetaModel

@EpoxyModelClass(layout = R.layout.recycler_anime_recent_sub_dub_2)
abstract class AnimeSubDubModel2 : EpoxyModelWithHolder<AnimeSubDubModel2.SubDubHolder>() {

    @EpoxyAttribute
    lateinit var animeMetaModel: AnimeMetaModel

    @EpoxyAttribute
    lateinit var clickListener: View.OnClickListener

    override fun bind(holder: SubDubHolder) {
        Glide.with(holder.binding.animeImage.context).load(animeMetaModel.imageUrl)
            .into(holder.binding.animeImage)
        holder.binding.animeTitle.text = animeMetaModel.title
        holder.binding.episodeNumber.text = animeMetaModel.episodeNumber
        holder.binding.root.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition =
            holder.binding.animeTitle.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeMetaModel.title}_${animeMetaModel.ID}"
        holder.binding.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition =
            holder.binding.animeImage.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeMetaModel.imageUrl}_${animeMetaModel.ID}"
        holder.binding.animeImage.transitionName = animeImageTransition

    }

    class SubDubHolder : EpoxyHolder() {
        lateinit var binding: RecyclerAnimeRecentSubDub2Binding

        override fun bindView(itemView: View) {
            binding = RecyclerAnimeRecentSubDub2Binding.bind(itemView)
        }
    }
}