package net.xblacky.animexstream.ui.main.player

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerControlView
import net.xblacky.animexstream.databinding.ExoPlayerCustomControlsBinding
import net.xblacky.animexstream.utils.animation.CustomAnimation
import net.xblacky.animexstream.utils.model.Content

@UnstableApi
class AnimePlayerControlView(context: Context, attrs: AttributeSet? = null) :
    PlayerControlView(context, attrs) {

    companion object {
        private const val SEEK_DISTANCE = 10000L
    }

    private val binding =
        ExoPlayerCustomControlsBinding.inflate(LayoutInflater.from(context), this, true)

    private var onEpisodeJumpListener: OnEpisodeJumpListener? = null
    private var onQualitySelectionClickListener: ((View) -> Unit)? = null
    private var onSpeedSelectionClickListener: ((View) -> Unit)? = null

    private val handleForward: OnClickListener = OnClickListener {
        seekPlayerForward()
        CustomAnimation.rotateForward(binding.exoFfwd)
        CustomAnimation.forwardAnimate(
            binding.exoForwardPlus,
            binding.exoForwardText
        )
    }
    private val handleRewind: OnClickListener = OnClickListener {
        seekPlayerBackward()
        CustomAnimation.rotateBackward(binding.exoRew)
        CustomAnimation.rewindAnimate(
            binding.exoRewindMinus,
            binding.exoRewindText
        )
    }
    private val handleNextEpisode: OnClickListener = OnClickListener {
        onEpisodeJumpListener?.onClickNextEpisode()
    }
    private val handlePreviousEpisode: OnClickListener = OnClickListener {
        onEpisodeJumpListener?.onClickPreviousEpisode()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setClickListeners()
    }

    override fun setPlayer(player: Player?) {
        super.setPlayer(player)
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                binding.exoPlay.isVisible = !isPlaying
                binding.exoPause.isVisible = isPlaying
            }
        })
    }

    private fun setClickListeners() {
        binding.exoPlay.setOnClickListener { player?.play() }
        binding.exoPause.setOnClickListener { player?.pause() }
        binding.exoRew.setOnClickListener(handleRewind)
        binding.exoFfwd.setOnClickListener(handleForward)
        binding.nextEpisode.setOnClickListener(handleNextEpisode)
        binding.previousEpisode.setOnClickListener(handlePreviousEpisode)
        binding.exoTrackSelectionView.setOnClickListener(onQualitySelectionClickListener)
        binding.exoSpeedSelectionView.setOnClickListener(onSpeedSelectionClickListener)
    }

    fun setonEpisodeJumpListener(onEpisodeJumpListener: OnEpisodeJumpListener) {
        this.onEpisodeJumpListener = onEpisodeJumpListener
    }

    fun setonQualitySelectionClickListener(onQualitySelectionClickListener: (View) -> Unit) {
        this.onQualitySelectionClickListener = onQualitySelectionClickListener
    }

    fun setonSpeedSelectionClickListener(onSpeedSelectionClickListener: (View) -> Unit) {
        this.onSpeedSelectionClickListener = onSpeedSelectionClickListener
    }

    fun setContent(content: Content) {
        binding.animeName.text = content.animeName
        binding.episodeName.text = content.episodeName
        binding.nextEpisode.isVisible = !content.nextEpisodeUrl.isNullOrBlank()
        binding.previousEpisode.isVisible = !content.previousEpisodeUrl.isNullOrBlank()
    }

    fun setQualityInfo(quality: String) {
        binding.exoQuality.text = quality
    }

    fun setSpeedInfo(speedText: String) {
        binding.exoSpeedText.text = speedText
    }

    fun setWatchedDuration(remainingTime: String) {
        binding.exoRemainingTime.text = remainingTime
    }

    private fun seekPlayerForward() {
        player?.let { player ->
            val isSeekForwardAvailable = (player.duration - player.currentPosition) > SEEK_DISTANCE
            if (isSeekForwardAvailable) {
                player.seekTo(player.currentPosition + SEEK_DISTANCE)
            } else {
                player.seekTo(player.duration)
            }
        }
    }

    private fun seekPlayerBackward() {
        player?.let { player ->
            val isSeekBackwardAvailable = (player.currentPosition - SEEK_DISTANCE) > 0
            if (isSeekBackwardAvailable) {
                player.seekTo(player.currentPosition - SEEK_DISTANCE)
            } else {
                player.seekTo(0L)
            }
        }
    }

    interface OnEpisodeJumpListener {
        fun onClickNextEpisode()
        fun onClickPreviousEpisode()
    }
}