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
import com.google.android.material.textview.MaterialTextView
import net.xblacky.animexstream.R
import net.xblacky.animexstream.utils.model.AnimeDisplayModel
import net.xblacky.animexstream.utils.model.AnimeMetaModel
import net.xblacky.animexstream.utils.tags.GenreTags
import org.apmem.tools.layouts.FlowLayout

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

        lateinit var animeType: MaterialTextView

        override fun bindView(itemView: View) {
            animeType = itemView.findViewById(R.id.typeName)
        }

    }
}


