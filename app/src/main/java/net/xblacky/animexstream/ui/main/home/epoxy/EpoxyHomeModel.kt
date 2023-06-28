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


@EpoxyModelClass(layout = R.layout.recycler_anime_recent_sub_dub_2)
abstract class AnimeSubDubModel2 : EpoxyModelWithHolder<AnimeSubDubModel2.SubDubHolder>() {

    @EpoxyAttribute
    lateinit var animeMetaModel: AnimeMetaModel

    @EpoxyAttribute
    lateinit var clickListener: View.OnClickListener

    override fun bind(holder: SubDubHolder) {
        Glide.with(holder.animeImageView.context).load(animeMetaModel.imageUrl)
            .into(holder.animeImageView)
        holder.animeTitle.text = animeMetaModel.title
        holder.animeEpisode.text = animeMetaModel.episodeNumber
        holder.rootLayout.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition = holder.animeTitle.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeMetaModel.title}_${animeMetaModel.ID}"
        holder.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition =
            holder.animeImageView.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeMetaModel.imageUrl}_${animeMetaModel.ID}"
        holder.animeImageView.transitionName = animeImageTransition

    }

    class SubDubHolder : EpoxyHolder() {

        lateinit var animeImageView: AppCompatImageView
        lateinit var animeTitle: TextView
        lateinit var animeEpisode: TextView
        lateinit var rootLayout: View

        override fun bindView(itemView: View) {
            rootLayout = itemView.findViewById(R.id.rootLayout)
            animeImageView = itemView.findViewById(R.id.animeImage)
            animeTitle = itemView.findViewById(R.id.animeTitle)
            animeEpisode = itemView.findViewById(R.id.episodeNumber)
        }

    }
}

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
        holder.animeEpisode.text = animeMetaModel.episodeNumber

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
        holder.rootView.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition = holder.rootView.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeMetaModel.title}_${animeMetaModel.ID}"
        holder.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition = holder.rootView.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeMetaModel.imageUrl}_${animeMetaModel.ID}"
        holder.animeImageView.transitionName = animeImageTransition

    }

    class PopularHolder : EpoxyHolder() {

        lateinit var animeImageView: AppCompatImageView
        lateinit var animeTitle: TextView
        lateinit var animeEpisode: TextView
        lateinit var flowLayout: FlowLayout
        lateinit var rootView: ConstraintLayout

        override fun bindView(itemView: View) {
            animeImageView = itemView.findViewById(R.id.animeImage)
            animeTitle = itemView.findViewById(R.id.animeTitle)
            animeEpisode = itemView.findViewById(R.id.episodeNumber)
            flowLayout = itemView.findViewById(R.id.flowLayout)
            rootView = itemView.findViewById(R.id.rootLayout)
        }

    }
}


@EpoxyModelClass(layout = R.layout.recycler_anime_mini_header)
abstract class AnimeMiniHeaderModel :
    EpoxyModelWithHolder<AnimeMiniHeaderModel.AnimeMiniHeaderHolder>() {

    @EpoxyAttribute
    lateinit var typeName: String

    override fun bind(holder: AnimeMiniHeaderHolder) {
        super.bind(holder)
        holder.animeType.text = typeName
    }


    class AnimeMiniHeaderHolder : EpoxyHolder() {

        lateinit var animeType: TextView

        override fun bindView(itemView: View) {
            animeType = itemView.findViewById(R.id.typeName)
        }

    }

}


