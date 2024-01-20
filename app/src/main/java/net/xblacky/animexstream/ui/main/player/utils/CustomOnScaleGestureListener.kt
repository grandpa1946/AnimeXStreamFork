package net.xblacky.animexstream.ui.main.player.utils

import android.view.ScaleGestureDetector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import net.xblacky.animexstream.utils.animation.CustomAnimation
import net.xblacky.animexstream.utils.touchevents.TouchUtils

class CustomOnScaleGestureListener(
    private val player: PlayerView
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var scaleFactor = 0f

    private var scaleByFloat = 1f

    override fun onScale(
        detector: ScaleGestureDetector
    ): Boolean {
        scaleFactor = detector.scaleFactor
        return true
    }

    override fun onScaleBegin(
        detector: ScaleGestureDetector
    ): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        if (scaleFactor > 1) {
            if (player.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                scaleByFloat =
                    TouchUtils.calculateScale(player = player)
                CustomAnimation.zoomInByScale(scaleBy = scaleByFloat, player = player)
                player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL


            }
        } else {
            if (player.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                CustomAnimation.zoomOutByScale(scaleBy = scaleByFloat, player = player)
                player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT


            }

        }
    }
}