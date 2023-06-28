package net.xblacky.animexstream.ui.main.player

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.xblacky.animexstream.R
import net.xblacky.animexstream.databinding.ActivityVideoPlayerBinding
import net.xblacky.animexstream.utils.model.Content
import net.xblacky.animexstream.utils.preference.Preference
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class VideoPlayerActivity : AppCompatActivity(), VideoPlayerListener {

    private val viewModel: VideoPlayerViewModel by viewModels()

    @Inject
    lateinit var preference: Preference
    private var episodeNumber: String? = ""
    private var animeName: String? = ""
    private lateinit var content: Content

    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addVideoPlayerFragment()
        getExtra(intent)
        setObserver()
        goFullScreen()
    }

    private fun addVideoPlayerFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(binding.playerActivityContainer.id, VideoPlayerFragment())
        transaction.commit()
    }


    override fun onNewIntent(intent: Intent?) {
        getPlayerFragment()?.let { playerFragment ->
            playerFragment.playOrPausePlayer(
                playWhenReady = false,
                loseAudioFocus = false
            )
            playerFragment.saveWatchedDuration()
        }

        getExtra(intent)
        super.onNewIntent(intent)
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

    private fun setObserver() {
        viewModel.content.observe(this) {
            this.content = it
            it?.let {
                if (it.urls.isNotEmpty()) {
//                    getPlayerFragment()?.updateContent(it)
                }
            }
        }
        viewModel.isLoading.observe(this) {
//            getPlayerFragment()?.showLoading(it.isLoading)
        }
        viewModel.errorModel.observe(this) {
            /*getPlayerFragment()?.showErrorLayout(
                it.show,
                it.errorMsgId,
                it.errorCode
            )*/
        }

        viewModel.cdnServer.observe(this) {
            Timber.e("Referrer : $it")
            preference.setReferrer(it)
        }
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

    private fun getPlayerFragment() =
        supportFragmentManager.findFragmentByTag(getString(R.string.video_player_fragment_tag)) as? VideoPlayerFragment
}