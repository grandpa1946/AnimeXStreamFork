package net.xblacky.animexstream.ui.main.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.error_screen_video_player.view.*
import kotlinx.android.synthetic.main.exo_player_custom_controls.*
import kotlinx.android.synthetic.main.exo_player_custom_controls.view.*
import kotlinx.android.synthetic.main.fragment_video_player.*
import kotlinx.android.synthetic.main.fragment_video_player.view.*
import kotlinx.android.synthetic.main.fragment_video_player_placeholder.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.xblacky.animexstream.R
import net.xblacky.animexstream.ui.main.player.di.PlayerDI
import net.xblacky.animexstream.ui.main.player.utils.CustomOnScaleGestureListener
import net.xblacky.animexstream.utils.Utils
import net.xblacky.animexstream.utils.animation.CustomAnimation
import net.xblacky.animexstream.utils.constants.C.Companion.ERROR_CODE_DEFAULT
import net.xblacky.animexstream.utils.constants.C.Companion.NO_INTERNET_CONNECTION
import net.xblacky.animexstream.utils.constants.C.Companion.RESPONSE_UNKNOWN
import net.xblacky.animexstream.utils.model.Content
import net.xblacky.animexstream.utils.preference.Preference
import net.xblacky.animexstream.utils.touchevents.TouchUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject


@AndroidEntryPoint
class VideoPlayerFragment : Fragment(), View.OnClickListener, Player.Listener,
    AudioManager.OnAudioFocusChangeListener {


    companion object {
        private val TAG = VideoPlayerFragment::class.java.simpleName
    }

    @Inject
    @PlayerDI.ExoOkHttpClient
    lateinit var okHttpClient: OkHttpClient

    private lateinit var videoUrl: String
    private lateinit var rootView: View
    private lateinit var player: ExoPlayer
    private lateinit var trackSelectionFactory: ExoTrackSelection.Factory
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private val SEEK_DISTANCE = 10000L

    private var mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? = null
    private lateinit var audioManager: AudioManager
    private lateinit var mFocusRequest: AudioFocusRequest
    private lateinit var content: Content
    private val DEFAULT_MEDIA_VOLUME = 1f
    private val DUCK_MEDIA_VOLUME = 0.2f
    private lateinit var handler: Handler
    private var isVideoPlaying: Boolean = false
    private var IS_MEDIA_M3U8 = false

    private lateinit var sharedPreferences: Preference

    private val speeds = arrayOf(0.5f, 1f, 1.25f, 1.5f, 2f)
    private val showableSpeed = arrayOf("0.50x", "1x", "1.25x", "1.50x", "2x")
    private var checkedItem = 1
    private var selectedSpeed = 1
    private var selectedQuality = 0
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        rootView = inflater.inflate(R.layout.fragment_video_player, container, false)
        setClickListeners()
        initializeAudioManager()
        initializePlayer()
        sharedPreferences = Preference(requireContext())
        return rootView
    }

    override fun onStart() {
        super.onStart()
        registerMediaSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGestureDetector()
        setupKeyListener()
    }

    override fun onDestroy() {
        player.release()
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        super.onDestroy()
    }

    private fun initializePlayer() {
        trackSelectionFactory = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(requireContext(), trackSelectionFactory)
        rootView.exoPlayerFrameLayout.setAspectRatio(16f / 9f)
        player = ExoPlayer.Builder(requireContext()).setTrackSelector(trackSelector).build()
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build()

        player.playWhenReady = true
        player.setAudioAttributes(audioAttributes, false)
        player.addListener(this)
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        rootView.exoPlayerView.player = player


    }

    private fun setGestureDetector() {
        val scaleGestureDetector =
            ScaleGestureDetector(requireContext(), CustomOnScaleGestureListener(exoPlayerView))
        exoPlayerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event?.let { TouchUtils.isAClick(event = it) } == true) {
                exoPlayerView.performClick()
            }
            true
        }
    }

    private fun setClickListeners() {
        rootView.exo_track_selection_view.setOnClickListener(this)
        rootView.exo_speed_selection_view.setOnClickListener(this)
        rootView.exo_rew.setOnClickListener(this)
        rootView.exo_ffwd.setOnClickListener(this)
        rootView.errorButton.setOnClickListener(this)
        rootView.back.setOnClickListener(this)
        rootView.nextEpisode.setOnClickListener(this)
        rootView.previousEpisode.setOnClickListener(this)
    }

    private fun buildMediaSource(url: String): MediaSource {
        val lastPath = Uri.parse(url).lastPathSegment
        IS_MEDIA_M3U8 = lastPath!!.contains("m3u8")
        val defaultDataSourceFactory = {
            val dataSource: DataSource.Factory =
                OkHttpDataSource.Factory(okHttpClient)
            dataSource.createDataSource()

        }
        return if (IS_MEDIA_M3U8) {
            HlsMediaSource.Factory(defaultDataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            return ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        }

    }

    fun updateContent(content: Content) {
        Timber.e("Content Updated uRL: ${content.urls}")
        this.content = content
        animeName.text = content.animeName
        val text = content.episodeName
        episodeName.text = text
        exoPlayerView.videoSurfaceView?.visibility = View.GONE

        this.content.nextEpisodeUrl?.let {
            nextEpisode.visibility = View.VISIBLE
        } ?: kotlin.run {
            nextEpisode.visibility = View.GONE
        }
        this.content.previousEpisodeUrl?.let {
            previousEpisode.visibility = View.VISIBLE
        } ?: kotlin.run {
            previousEpisode.visibility = View.GONE
        }

        if (!content.urls.isNullOrEmpty()) {
            try {
                updateVideoUrl(content.urls[selectedQuality].url)
                updateQualityText(selectedQuality)
            } catch (exc: IndexOutOfBoundsException) {
                updateVideoUrl(content.urls[0].url)
                updateQualityText()
            }

        } else {
            showErrorLayout(
                show = true,
                errorCode = RESPONSE_UNKNOWN,
                errorMsgId = R.string.server_error
            )
        }

    }

    private fun updateVideoUrl(videoUrl: String, seekTo: Long? = content.watchedDuration) {
        this.videoUrl = videoUrl
        loadVideo(seekTo = seekTo)
    }

    private fun loadVideo(seekTo: Long? = 0, playWhenReady: Boolean = true) {
        player.playWhenReady = playWhenReady
        showLoading(true)
        showErrorLayout(false, 0, 0)
        val mediaSource = buildMediaSource(videoUrl)
        player.setMediaSource(mediaSource)
        player.prepare()
        seekTo?.let {
            Timber.e(it.toString())
            player.seekTo(it)
        }


    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.exo_track_selection_view -> {
                showDialogForQualitySelection()
            }
            R.id.exo_speed_selection_view -> {
                showDialogForSpeedSelection()
            }
            R.id.errorButton -> {
                refreshData()
            }
            R.id.back -> {
                (activity as VideoPlayerActivity).enterPipModeOrExit()
            }
            R.id.nextEpisode -> {
                playNextEpisode()
            }
            R.id.exo_ffwd -> {
                seekForward()
            }
            R.id.exo_rew -> {
                seekRewind()
            }
            R.id.previousEpisode -> {
                playPreviousEpisode()
            }
        }
    }

    private fun showM3U8TrackSelector() {
        mappedTrackInfo = trackSelector?.currentMappedTrackInfo

        try {
            TrackSelectionDialogBuilder(
                requireContext(),
                getString(R.string.video_quality),
                trackSelector,
                0

            ).build().show()
        } catch (ignored: java.lang.NullPointerException) {
        }

    }


    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
        if (IS_MEDIA_M3U8) {
            try {
                val videoQuality =
                    trackSelections.get(0)!!.getFormat(0).height.toString() + "p"
                val quality = "Quality($videoQuality)"
                Timber.e("Quality $quality")
                rootView.exoQuality.text = quality
            } catch (ignored: Exception) {
            }

        }
    }

    private fun listenToRemaningTime() {
        job = lifecycleScope.launch {
            while (isVideoPlaying) {
                val totalDuration = player.duration
                val watchedDuration = player.currentPosition
                val remainingTime = Utils.getRemainingTime(
                    watchedDuration = watchedDuration,
                    totalDuration = totalDuration
                )
                exo_remaining_time.text = remainingTime

                delay(1000L)
            }
        }
    }

    private fun refreshData() {
        if (::content.isInitialized && content.urls.isNotEmpty()) {
            loadVideo(player.currentPosition, true)
        } else {
            (activity as VideoPlayerActivity).refreshM3u8Url()
        }

    }

    private fun seekForward() {
        seekExoPlayerForward()
        CustomAnimation.rotateForward(exo_ffwd)
        CustomAnimation.forwardAnimate(exo_forward_plus, exo_forward_text)
    }

    private fun seekExoPlayerForward() {
        val isSeekForwardAvailable = (player.duration - player.currentPosition) > SEEK_DISTANCE
        if (isSeekForwardAvailable) {
            player.seekTo(player.currentPosition + SEEK_DISTANCE)
        } else {
            player.seekTo(player.duration)
        }
    }

    private fun seekRewind() {
        seekExoPlayerBackward()
        CustomAnimation.rotateBackward(exo_rew)
        CustomAnimation.rewindAnimate(exo_rewind_minus, exo_rewind_text)
    }

    private fun seekExoPlayerBackward() {
        val isSeekBackwardAvailable = (player.currentPosition - SEEK_DISTANCE) > 0
        if (isSeekBackwardAvailable) {
            player.seekTo(player.currentPosition - SEEK_DISTANCE)
        } else {
            player.seekTo(0L)
        }

    }

    private fun playNextEpisode() {
        playOrPausePlayer(playWhenReady = false, loseAudioFocus = false)
        saveWatchedDuration()
        showLoading(true)
        (activity as VideoPlayerListener).playNextEpisode()

    }

    private fun playPreviousEpisode() {
        playOrPausePlayer(playWhenReady = false, loseAudioFocus = false)
        showLoading(true)
        saveWatchedDuration()
        (activity as VideoPlayerListener).playPreviousEpisode()

    }

    fun showLoading(showLoading: Boolean) {
        if (::rootView.isInitialized) {
            if (showLoading) {
                rootView.videoPlayerLoading.visibility = View.VISIBLE
            } else {
                rootView.videoPlayerLoading.visibility = View.GONE
            }
        }
    }


    fun showErrorLayout(show: Boolean, errorMsgId: Int, errorCode: Int) {
        if (show) {
            rootView.errorLayout.visibility = View.VISIBLE
            context.let {
                rootView.errorText.text = getString(errorMsgId)
                when (errorCode) {
                    ERROR_CODE_DEFAULT -> {
                        rootView.errorImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_error,
                                null
                            )
                        )
                    }
                    RESPONSE_UNKNOWN -> {
                        rootView.errorImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_error,
                                null
                            )
                        )
                    }
                    NO_INTERNET_CONNECTION -> {
                        rootView.errorImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_internet,
                                null
                            )
                        )
                    }
                }
            }
        } else {
            rootView.errorLayout.visibility = View.GONE
        }
    }


    private fun showDialogForQualitySelection() {
        if (IS_MEDIA_M3U8) {
            showM3U8TrackSelector()
        } else {
            val builder = AlertDialog.Builder(requireContext())
            builder.apply {
                setTitle("Quality")
                setSingleChoiceItems(
                    getQualityArray().toTypedArray(),
                    selectedQuality
                ) { dialog, selectedIndex ->
                    selectQuality(selectedIndex)
                    dialog.dismiss()
                }
                builder.create().show()
            }
        }
    }

    private fun selectQuality(index: Int) {
        selectedQuality = index
        updateQualityText(index = index)
        if (content.urls[index].url != videoUrl) {
            updateVideoUrl(content.urls[index].url, player.currentPosition)
        }

    }

    private fun updateQualityText(index: Int = 0) {
        selectedQuality = index
        val quality = "Quality(${content.urls[index].label})"
        rootView.exoQuality.text = quality
    }

    private fun getQualityArray(): ArrayList<String> {
        val list = ArrayList<String>()
        if (::content.isInitialized) {
            content.urls.forEach {
                list.add(it.label)
            }
        }
        return list
    }

    // set playback speed for exoplayer
    private fun setPlaybackSpeed(speed: Float) {
        val params = PlaybackParameters(speed)
        player.playbackParameters = params
    }

    // set the speed, selectedItem and change the text
    private fun setSpeed(speed: Int) {
        selectedSpeed = speed
        checkedItem = speed
        val speedText = "Speed(${showableSpeed[speed]})"
        exoSpeedText.text = speedText
        setPlaybackSpeed(speeds[selectedSpeed])
    }

    // show dialog to select the speed.
    private fun showDialogForSpeedSelection() {
        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle("Playback speed")
            setSingleChoiceItems(showableSpeed, checkedItem) { dialog, which ->
                setSpeed(which)
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onPlayerError(error: PlaybackException) {
        isVideoPlaying = false
        val cause = error.cause
        if (cause is HttpDataSource.HttpDataSourceException) {
            // An HTTP error occurred.
            val httpError: HttpDataSource.HttpDataSourceException = cause
            // This is the request for which the error occurred.
            // querying the cause.
            if (httpError is InvalidResponseCodeException) {
                val responseCode = httpError.responseCode
                content.urls = ArrayList()
                showErrorLayout(
                    show = true,
                    errorMsgId = R.string.server_error,
                    errorCode = RESPONSE_UNKNOWN
                )

                Timber.e("Response Code $responseCode")
                // message and headers.
            } else {
                showErrorLayout(
                    show = true,
                    errorMsgId = R.string.no_internet,
                    errorCode = NO_INTERNET_CONNECTION
                )
            }
        }

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        isVideoPlaying = playWhenReady
        if (isVideoPlaying) listenToRemaningTime() else job?.cancel()
        if (playbackState == Player.STATE_READY && playWhenReady) {
            rootView.exo_play.setImageResource(R.drawable.ic_media_play)
            rootView.exo_pause.setImageResource(R.drawable.ic_media_pause)

            playOrPausePlayer(true)

        }
        if (playbackState == Player.STATE_BUFFERING && playWhenReady) {
            rootView.exo_play.setImageResource(0)
            rootView.exo_pause.setImageResource(0)
            showLoading(false)
        }
        if (playbackState == Player.STATE_READY) {
            exoPlayerView.videoSurfaceView?.visibility = View.VISIBLE
        }
    }


    private fun initializeAudioManager() {
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val mAudioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mAudioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(this)
                .build()
        }

    }


    private fun requestAudioFocus(): Boolean {

        val focusRequest: Int

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (::audioManager.isInitialized && ::mFocusRequest.isInitialized) {
                focusRequest = audioManager.requestAudioFocus(mFocusRequest)
                checkFocusRequest(focusRequest = focusRequest)
            } else {
                false
            }

        } else {
            focusRequest = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            checkFocusRequest(focusRequest)
        }

    }

    private fun checkFocusRequest(focusRequest: Int): Boolean {
        return when (focusRequest) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
            else -> false
        }
    }

    private fun loseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(mFocusRequest)
        } else {
            audioManager.abandonAudioFocus(this)
        }
    }

    fun playOrPausePlayer(playWhenReady: Boolean, loseAudioFocus: Boolean = true) {
        if (playWhenReady && requestAudioFocus()) {
            player.playWhenReady = true
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            player.playWhenReady = false
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (loseAudioFocus) {
                loseAudioFocus()
            }
        }
    }

    override fun onStop() {
        saveWatchedDuration()
        if (::content.isInitialized) {
            (activity as VideoPlayerListener).updateWatchedValue(content)
        }
        playOrPausePlayer(false)
        unRegisterMediaSession()
        super.onStop()
    }

    override fun onAudioFocusChange(focusChange: Int) {

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = DEFAULT_MEDIA_VOLUME
                playOrPausePlayer(true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                playOrPausePlayer(false, loseAudioFocus = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.volume = DUCK_MEDIA_VOLUME
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                playOrPausePlayer(false)
            }
        }
    }

    private fun registerMediaSession() {
        mediaSession = MediaSessionCompat(requireContext(), TAG)
//        if (::content.isInitialized) {
//
////            val mediaMetadataCompat = MediaMetadataCompat.Builder()
////                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, content.title)
////                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, resources.getString(R.string.app_name))
//////                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(resources, R.drawable.app_icon))
////                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.title)
////                    .build()
////
////            mediaSession.setMetadata(mediaMetadataCompat)
//        }
        mediaSession.isActive = true
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
    }

    private fun unRegisterMediaSession() {
        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
    }

    fun saveWatchedDuration() {
        if (::content.isInitialized) {
            val watchedDuration = player.currentPosition
            content.duration = player.duration
            content.watchedDuration = watchedDuration
            if (watchedDuration > 0) {
                (activity as VideoPlayerListener).updateWatchedValue(content)
            }
        }
    }

    fun isVideoPlaying(): Boolean {
        return isVideoPlaying
    }

    private fun setupKeyListener() {
        // Set the key listener for the root view
        exoPlayerFrameLayout.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!exoPlayerView.isControllerVisible) {
                            exoPlayerView.showController()
                        }

                        // Move focus to the element above the currently focused view
                        val viewAbove = v.focusSearch(View.FOCUS_UP)
                        viewAbove?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!exoPlayerView.isControllerVisible) {
                            exoPlayerView.showController()
                        }

                        // Move focus to the element below the currently focused view
                        val viewBelow = v.focusSearch(View.FOCUS_DOWN)
                        viewBelow?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Move focus to the element to the left of the currently focused view
                        if (exoPlayerView.isControllerVisible) {
                            val viewLeft = v.focusSearch(View.FOCUS_LEFT)
                            viewLeft?.requestFocus()
                            return@setOnKeyListener true
                        } else {
                            seekRewind()
                        }
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // Move focus to the element to the right of the currently focused view
                        if (exoPlayerView.isControllerVisible) {
                            val viewRight = v.focusSearch(View.FOCUS_RIGHT)
                            viewRight?.requestFocus()
                            return@setOnKeyListener true
                        } else {
                            seekForward()
                        }
                    }
                }
            }
            return@setOnKeyListener false
        }
    }
}

interface VideoPlayerListener {
    fun updateWatchedValue(content: Content)
    fun playPreviousEpisode()
    fun playNextEpisode()
}