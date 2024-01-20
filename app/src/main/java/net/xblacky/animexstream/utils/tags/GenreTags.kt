package net.xblacky.animexstream.utils.tags

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import net.xblacky.animexstream.databinding.TagsGenreBinding

class GenreTags {

    fun getGenreTag(context: Context, genreName: String, genreUrl: String): View {
        val binding = TagsGenreBinding.inflate(LayoutInflater.from(context))
        binding.genre.apply {
            text = genreName
            maxLines = 1
            val relButton1 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            relButton1.setMargins(8, 8, 8, 8)
            layoutParams = relButton1
        }

        return binding.root
    }

}