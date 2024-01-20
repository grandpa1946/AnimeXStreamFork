package net.xblacky.animexstream.ui.main.animeinfo.epoxy

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import net.xblacky.animexstream.R

@EpoxyModelClass(layout = R.layout.recycler_episode_item)
abstract class EpisodeModel : EpoxyModelWithHolder<EpisodeModel.HomeHeaderHolder>() {

    @EpoxyAttribute
    lateinit var episodeModel: net.xblacky.animexstream.utils.model.EpisodeModel

    @EpoxyAttribute
    lateinit var clickListener: View.OnClickListener

    @EpoxyAttribute
    var watchedProgress: Long = 0


    override fun bind(holder: HomeHeaderHolder) {
        super.bind(holder)
        holder.episodeText.text = episodeModel.episodeNumber
        holder.cardView.setOnClickListener(clickListener)
        holder.progressBar.progress =
            if (watchedProgress > 90) 100 else if (watchedProgress in 1..10) 10 else watchedProgress.toInt()
        holder.cardView.setCardBackgroundColor(
            ResourcesCompat.getColor(
                holder.cardView.resources,
                R.color.episode_background,
                null
            )
        )


    }

    class HomeHeaderHolder : EpoxyHolder() {
        lateinit var episodeText: MaterialTextView
        lateinit var cardView: MaterialCardView
        lateinit var progressBar: LinearProgressIndicator

        override fun bindView(itemView: View) {
            episodeText = itemView.findViewById(R.id.episodeNumber)
            cardView = itemView.findViewById(R.id.cardView)
            progressBar = itemView.findViewById(R.id.watchedProgress)
        }
    }

}