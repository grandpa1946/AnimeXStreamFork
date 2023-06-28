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
import net.xblacky.animexstream.utils.model.AnimeMetaModel
import net.xblacky.animexstream.utils.tags.GenreTags
import org.apmem.tools.layouts.FlowLayout

@EpoxyModelClass(layout = R.layout.recycler_anime_popular)
abstract class AnimePopularModel : EpoxyModelWithHolder<AnimePopularModel.PopularHolder>() {

    @EpoxyAttribute
    lateinit var animeMetaModel: AnimeMetaModel

    @EpoxyAttribute
    lateinit var clickListener: View.OnClickListener

    override fun bind(holder: PopularHolder) {
        Glide.with(holder.animeImageView.context).load(animeMetaModel.imageUrl).centerCrop()
            .into(holder.animeImageView)
        holder.animeTitle.text = animeMetaModel.title
        holder.episodeNumber.text = animeMetaModel.episodeNumber

        holder.flowLayout.removeAllViews()

        animeMetaModel.genreList?.forEach {
            holder.flowLayout.addView(
                GenreTags().getGenreTag(
                    holder.flowLayout.context,
                    it.genreName,
                    it.genreUrl
                )
            )
        }
        holder.root.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition =
            holder.root.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeMetaModel.title}_${animeMetaModel.ID}"
        holder.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition =
            holder.root.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeMetaModel.imageUrl}_${animeMetaModel.ID}"
        holder.animeImageView.transitionName = animeImageTransition

    }


    class PopularHolder : EpoxyHolder() {

        lateinit var animeImageView: AppCompatImageView
        lateinit var animeTitle: TextView
        lateinit var root: ConstraintLayout
        lateinit var flowLayout: FlowLayout
        lateinit var episodeNumber: TextView

        override fun bindView(itemView: View) {
            animeImageView = itemView.findViewById(R.id.animeImage)
            animeTitle = itemView.findViewById(R.id.animeTitle)
            root = itemView.findViewById(R.id.root)
            flowLayout = itemView.findViewById(R.id.flowLayout)
            episodeNumber = itemView.findViewById(R.id.episodeNumber)
        }
    }
}