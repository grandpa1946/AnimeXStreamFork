package net.xblacky.animexstream.ui.main.home.epoxy

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
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
        Glide.with(holder.animeImage.context).load(animeMetaModel.imageUrl)
            .into(holder.animeImage)
        holder.animeTitle.text = animeMetaModel.title
        holder.episodeNumber.text = animeMetaModel.episodeNumber
        holder.root.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition =
            holder.animeTitle.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeMetaModel.title}_${animeMetaModel.ID}"
        holder.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition =
            holder.animeImage.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeMetaModel.imageUrl}_${animeMetaModel.ID}"
        holder.animeImage.transitionName = animeImageTransition

    }

    class SubDubHolder : EpoxyHolder() {

        lateinit var animeImage: AppCompatImageView
        lateinit var animeTitle: TextView
        lateinit var episodeNumber: TextView
        lateinit var root: ConstraintLayout

        override fun bindView(itemView: View) {
            animeImage = itemView.findViewById(R.id.animeImage)
            animeTitle = itemView.findViewById(R.id.animeTitle)
            episodeNumber = itemView.findViewById(R.id.episodeNumber)
            root = itemView.findViewById(R.id.root)
        }

    }
}