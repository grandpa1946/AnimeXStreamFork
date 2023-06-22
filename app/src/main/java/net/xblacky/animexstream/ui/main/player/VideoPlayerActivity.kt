package net.xblacky.animexstream.ui.main.player

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_video_player.*
import kotlinx.android.synthetic.main.fragment_video_player.*
import net.xblacky.animexstream.R
import net.xblacky.animexstream.utils.model.Content
import timber.log.Timber
import java.lang.Exception
import android.view.WindowInsetsController

import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.database.android.AndroidPlatform
import kotlinx.android.synthetic.main.fragment_video_player.view.exoPlayerView
import net.xblacky.animexstream.utils.helper.isRunningOnAndroidTV
import net.xblacky.animexstream.utils.preference.Preference
import javax.inject.Inject


@AndroidEntryPoint
class VideoPlayerActivity : AppCompatActivity(), VideoPlayerListener {

    private val viewModel: VideoPlayerViewModel by viewModels()

    @Inject
    lateinit var preference: Preference
    private var episodeNumber: String? = ""
    private var animeName: String? = ""
    private lateinit var content: Content
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        getExtra(intent)
        setObserver()
        goFullScreen()
        setupKeyListener()
    }


    override fun onNewIntent(intent: Intent?) {
        (playerFragment as VideoPlayerFragment).playOrPausePlayer(
            playWhenReady = false,
            loseAudioFocus = false
        )
        (playerFragment as VideoPlayerFragment).saveWatchedDuration()
        getExtra(intent)
        super.onNewIntent(intent)

    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipMode()
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            goFullScreen()
        }
    }

    private fun getExtra(intent: Intent?) {
        val url = intent?.extras?.getString("episodeUrl")
        episodeNumber = intent?.extras?.getString("episodeNumber")
        animeName = intent?.extras?.getString("animeName")
        viewModel.updateEpisodeContent(
            Content(
                animeName = animeName ?: "",
                episodeUrl = url,
                episodeName = "\"$episodeNumber\"",
                urls = ArrayList()
            )
        )
        viewModel.fetchEpisodeData()
    }

    @Suppress("DEPRECATION")
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager
                .hasSystemFeature(
                    PackageManager.FEATURE_PICTURE_IN_PICTURE
                )
            && hasPipPermission()
            && (playerFragment as VideoPlayerFragment).isVideoPlaying()
            && !isRunningOnAndroidTV()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
        }
    }

    override fun onStop() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && hasPipPermission()
        ) {
            finishAndRemoveTask()
        }
        super.onStop()
    }

    override fun finish() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            finishAndRemoveTask()
        }
        super.finish()

        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_in_down)
    }

    fun enterPipModeOrExit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager
                .hasSystemFeature(
                    PackageManager.FEATURE_PICTURE_IN_PICTURE
                )
            && (playerFragment as VideoPlayerFragment).isVideoPlaying()
            && hasPipPermission()
        ) {
            try {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = PictureInPictureParams.Builder()
                    this.enterPictureInPictureMode(params.build())
                } else {
                    this.enterPictureInPictureMode()
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            }

        } else {
            finish()

        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        exoPlayerView.useController = !isInPictureInPictureMode
    }

    private fun hasPipPermission(): Boolean {
        val appsOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                appsOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                appsOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            else -> {
                false
            }
        }
    }

    private fun setObserver() {

        viewModel.content.observe(this, Observer {
            this.content = it
            it?.let {
                if (it.urls.isNotEmpty()) {
                    (playerFragment as VideoPlayerFragment).updateContent(it)
                }
            }
        })
        viewModel.isLoading.observe(this, Observer {
            (playerFragment as VideoPlayerFragment).showLoading(it.isLoading)
        })
        viewModel.errorModel.observe(this, Observer {
            (playerFragment as VideoPlayerFragment).showErrorLayout(
                it.show,
                it.errorMsgId,
                it.errorCode
            )
        })

        viewModel.cdnServer.observe(this) {
            Timber.e("Referrer : $it")
            preference.setReferrer(it)
        }
    }

    override fun onBackPressed() {
        enterPipModeOrExit()
    }

    private fun goFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

    }

    override fun updateWatchedValue(content: Content) {
        viewModel.saveContent(content)
    }

    override fun playNextEpisode() {
        viewModel.updateEpisodeContent(
            Content(
                episodeUrl = content.nextEpisodeUrl,
                episodeName = "\"EP ${incrementEpisodeNumber(content.episodeName!!)}\"",
                urls = ArrayList(),
                animeName = content.animeName
            )
        )
        viewModel.fetchEpisodeData()

    }

    override fun playPreviousEpisode() {

        viewModel.updateEpisodeContent(
            Content(
                episodeUrl = content.previousEpisodeUrl,
                episodeName = "\"EP ${decrementEpisodeNumber(content.episodeName!!)}\"",
                urls = ArrayList(),
                animeName = content.animeName
            )
        )
        viewModel.fetchEpisodeData()
    }

    private fun incrementEpisodeNumber(episodeName: String): String {
        return try {
            Timber.e("Episode Name $episodeName")
            val episodeString = episodeName.substring(
                episodeName.lastIndexOf(' ') + 1,
                episodeName.lastIndex
            )
            var episodeNumber = Integer.parseInt(episodeString)
            episodeNumber++
            episodeNumber.toString()

        } catch (obe: ArrayIndexOutOfBoundsException) {
            ""
        }
    }

    private fun decrementEpisodeNumber(episodeName: String): String {
        return try {
            val episodeString = episodeName.substring(
                episodeName.lastIndexOf(' ') + 1,
                episodeName.lastIndex
            )
            var episodeNumber = Integer.parseInt(episodeString)
            episodeNumber--
            episodeNumber.toString()

        } catch (obe: ArrayIndexOutOfBoundsException) {
            ""
        }
    }


    fun refreshM3u8Url() {
        viewModel.fetchEpisodeData(forceRefresh = true)
    }

    private fun setupKeyListener() {
        // Set the key listener for the root view
        exoPlayerView.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if(!exoPlayerView.isControllerVisible) {
                    exoPlayerView.showController()
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        // Move focus to the element above the currently focused view
                        val viewAbove = v.focusSearch(View.FOCUS_UP)
                        viewAbove?.requestFocus()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Move focus to the element below the currently focused view
                        val viewBelow = v.focusSearch(View.FOCUS_DOWN)
                        viewBelow?.requestFocus()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Move focus to the element to the left of the currently focused view
                        val viewLeft = v.focusSearch(View.FOCUS_LEFT)
                        viewLeft?.requestFocus()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // Move focus to the element to the right of the currently focused view
                        val viewRight = v.focusSearch(View.FOCUS_RIGHT)
                        viewRight?.requestFocus()
                        return@setOnKeyListener true
                    }
                }
            }
            return@setOnKeyListener false
        }
    }
}