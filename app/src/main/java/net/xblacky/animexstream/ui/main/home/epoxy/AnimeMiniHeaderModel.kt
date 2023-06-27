package net.xblacky.animexstream.ui.main.home.epoxy

import android.view.View
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import net.xblacky.animexstream.R
import net.xblacky.animexstream.databinding.RecyclerAnimeMiniHeaderBinding


@EpoxyModelClass(layout = R.layout.recycler_anime_mini_header)
abstract class AnimeMiniHeaderModel :
    EpoxyModelWithHolder<AnimeMiniHeaderModel.AnimeMiniHeaderHolder>() {

    @EpoxyAttribute
    lateinit var typeName: String

    override fun bind(holder: AnimeMiniHeaderHolder) {
        super.bind(holder)
        holder.binding.typeName.text = typeName
    }


    class AnimeMiniHeaderHolder : EpoxyHolder() {
        lateinit var binding: RecyclerAnimeMiniHeaderBinding

        override fun bindView(itemView: View) {
            binding = RecyclerAnimeMiniHeaderBinding.bind(itemView)
        }
    }
}


