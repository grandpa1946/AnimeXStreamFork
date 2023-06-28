package net.xblacky.animexstream.ui.main.home.epoxy

import android.view.View
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import net.xblacky.animexstream.R


@EpoxyModelClass(layout = R.layout.recycler_anime_mini_header)
abstract class AnimeMiniHeaderModel :
    EpoxyModelWithHolder<AnimeMiniHeaderModel.AnimeMiniHeaderHolder>() {

    @EpoxyAttribute
    lateinit var typeName: String

    override fun bind(holder: AnimeMiniHeaderHolder) {
        super.bind(holder)
        holder.typeName.text = typeName
    }

    class AnimeMiniHeaderHolder : EpoxyHolder() {

        lateinit var typeName: TextView

        override fun bindView(itemView: View) {
            typeName = itemView.findViewById(R.id.typeName)
        }

    }
}


