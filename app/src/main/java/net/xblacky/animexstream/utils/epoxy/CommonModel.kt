package net.xblacky.animexstream.utils.epoxy

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import net.xblacky.animexstream.R
import net.xblacky.animexstream.utils.model.AnimeDisplayModel

@EpoxyModelClass(layout = R.layout.recycler_anime_common)
abstract class AnimeCommonModel : EpoxyModelWithHolder<AnimeCommonModel.CommonAnimeViewHolder>() {

    @EpoxyAttribute
    lateinit var animeDisplayModel: AnimeDisplayModel

    @EpoxyAttribute
    lateinit var clickListener: View.OnClickListener

    override fun bind(holder: CommonAnimeViewHolder) {
        Glide.with(holder.animeImageView.context).load(animeDisplayModel.imageUrl)
            .into(holder.animeImageView)
        holder.animeTitle.text = animeDisplayModel.title
        holder.subtitleText.text = animeDisplayModel.subtitle
        holder.rootLayout.setOnClickListener(clickListener)

        //Set Shared Element for Anime Title
        var animeTitleTransition = holder.animeTitle.context.getString(R.string.shared_anime_title)
        animeTitleTransition =
            "${animeTitleTransition}_${animeDisplayModel.title}_${animeDisplayModel.id}"
        holder.animeTitle.transitionName = animeTitleTransition

        //Set Shared Element for Anime Image
        var animeImageTransition =
            holder.animeImageView.context.getString(R.string.shared_anime_image)
        animeImageTransition =
            "${animeImageTransition}_${animeDisplayModel.imageUrl}_${animeDisplayModel.id}"
        holder.animeImageView.transitionName = animeImageTransition

    }

    class CommonAnimeViewHolder : EpoxyHolder() {

        lateinit var animeImageView: AppCompatImageView
        lateinit var animeTitle: TextView
        lateinit var subtitleText: TextView
        lateinit var rootLayout: View

        override fun bindView(itemView: View) {
            rootLayout = itemView
            animeImageView = itemView.findViewById(R.id.animeImage)
            animeTitle = itemView.findViewById(R.id.animeTitle)
            subtitleText = itemView.findViewById(R.id.subtitleTextView)
        }

    }
}

@EpoxyModelClass(layout = R.layout.recycler_loading)
abstract class LoadingModel : EpoxyModelWithHolder<LoadingHolder>()

class LoadingHolder : EpoxyHolder() {
    override fun bindView(itemView: View) {
    }
}

