package net.xblacky.animexstream.ui.main.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.TrackSelectionDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.xblacky.animexstream.R
import net.xblacky.animexstream.databinding.FragmentVideoPlayerBinding
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
import timber.log.Timber
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class VideoPlayerFragment : Fragment(), View.OnClickListener, Player.Listener,
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        val TAG = VideoPlayerFragment::class.java.simpleName
        private const val SEEK_DISTANCE = 10000L
        private const val DEFAULT_MEDIA_VOLUME = 1f
        private const val DUCK_MEDIA_VOLUME = 0.2f
    }

    @Inject
    @PlayerDI.ExoOkHttpClient
    lateinit var okHttpClient: OkHttpClient

    private lateinit var videoUrl: String
    private lateinit var player: ExoPlayer
    private lateinit var trackSelectionFactory: ExoTrackSelection.Factory
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var mediaSession: MediaSession

    private var mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? = null
    private lateinit var audioManager: AudioManager
    private lateinit var mFocusRequest: AudioFocusRequest
    private lateinit var content: Content
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

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        setClickListeners()
        initializeAudioManager()
        initializePlayer()
        sharedPreferences = Preference(requireContext())
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        registerMediaSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGestureDetector()
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
        binding.exoPlayerFrameLayout.setAspectRatio(16f / 9f)
        player = ExoPlayer.Builder(requireContext()).setTrackSelector(trackSelector).build()
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        player.playWhenReady = true
        player.setAudioAttributes(audioAttributes, false)
        player.addListener(this)
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        binding.exoPlayerView.player = player
    }

    private fun setGestureDetector() {
        val scaleGestureDetector =
            ScaleGestureDetector(
                requireContext(),
                CustomOnScaleGestureListener(binding.exoPlayerView)
            )
        binding.exoPlayerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event?.let { TouchUtils.isAClick(event = it) } == true) {
                binding.exoPlayerView.performClick()
            }
            true
        }
    }

    private fun setClickListeners() {
        binding.lytCustomControls.exoTrackSelectionView.setOnClickListener(this)
        binding.lytCustomControls.exoSpeedSelectionView.setOnClickListener(this)
        binding.lytCustomControls.exoRew.setOnClickListener(this)
        binding.lytCustomControls.exoFfwd.setOnClickListener(this)
        binding.lytErrorScreenVideoPlayer.errorButton.setOnClickListener(this)
        binding.lytCustomControls.nextEpisode.setOnClickListener(this)
        binding.lytCustomControls.previousEpisode.setOnClickListener(this)
    }

    private fun buildMediaSource(url: String): MediaSource {
        val lastPath = Uri.parse(url).lastPathSegment
        IS_MEDIA_M3U8 = lastPath?.contains("m3u8") == true
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
        binding.lytCustomControls.animeName.text = content.animeName
        val text = content.episodeName
        binding.lytCustomControls.episodeName.text = text
        binding.exoPlayerView.videoSurfaceView?.visibility = View.GONE

        this.content.nextEpisodeUrl?.let {
            binding.lytCustomControls.nextEpisode.visibility = View.VISIBLE
        } ?: kotlin.run {
            binding.lytCustomControls.nextEpisode.visibility = View.GONE
        }
        this.content.previousEpisodeUrl?.let {
            binding.lytCustomControls.previousEpisode.visibility = View.VISIBLE
        } ?: kotlin.run {
            binding.lytCustomControls.previousEpisode.visibility = View.GONE
        }

        if (content.urls.isNotEmpty()) {
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
        mappedTrackInfo = trackSelector.currentMappedTrackInfo

        try {
            TrackSelectionDialogBuilder(
                requireContext(),
                getString(R.string.video_quality),
                player,
                0

            ).build().show()
        } catch (ignored: java.lang.NullPointerException) {
        }

    }

    override fun onTracksChanged(tracks: Tracks) {
        super.onTracksChanged(tracks)
        if (IS_MEDIA_M3U8) {
            try {
                val videoQuality =/*trackSelections.get(0)!!.getFormat(0).height.toString() +*/ "p"
                val quality = "Quality($videoQuality)"
                Timber.e("Quality $quality")
                binding.lytCustomControls.exoQuality.text = quality
            } catch (ignored: Exception) {
            }

        }
    }

    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        super.onTrackSelectionParametersChanged(parameters)
        if (IS_MEDIA_M3U8) {
            try {
                val videoQuality = parameters.viewportHeight.toString() + "p"
                val quality = "Quality($videoQuality)"
                Timber.e("Quality $quality")
                binding.lytCustomControls.exoQuality.text = quality
            } catch (ignored: Exception) {
            }

        }
    }

    private fun listenToRemainingTime() {
        job = lifecycleScope.launch {
            while (isVideoPlaying) {
                val totalDuration = player.duration
                val watchedDuration = player.currentPosition
                val remainingTime = Utils.getRemainingTime(
                    watchedDuration = watchedDuration,
                    totalDuration = totalDuration
                )
                binding.lytCustomControls.exoRemainingTime.text = remainingTime

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
        CustomAnimation.rotateForward(binding.lytCustomControls.exoFfwd)
        CustomAnimation.forwardAnimate(
            binding.lytCustomControls.exoForwardPlus,
            binding.lytCustomControls.exoForwardText
        )
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
        CustomAnimation.rotateBackward(binding.lytCustomControls.exoRew)
        CustomAnimation.rewindAnimate(
            binding.lytCustomControls.exoRewindMinus,
            binding.lytCustomControls.exoRewindText
        )
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
        if (showLoading) {
            binding.lytVideoPlayerPlaceholder.videoPlayerLoading.visibility = View.VISIBLE
        } else {
            binding.lytVideoPlayerPlaceholder.videoPlayerLoading.visibility = View.GONE
        }
    }


    fun showErrorLayout(show: Boolean, errorMsgId: Int, errorCode: Int) {
        if (show) {
            binding.lytErrorScreenVideoPlayer.errorLayout.visibility = View.VISIBLE
            context.let {
                binding.lytErrorScreenVideoPlayer.errorText.text = getString(errorMsgId)
                when (errorCode) {
                    ERROR_CODE_DEFAULT -> {
                        binding.lytErrorScreenVideoPlayer.errorImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_error,
                                null
                            )
                        )
                    }

                    RESPONSE_UNKNOWN -> {
                        binding.lytErrorScreenVideoPlayer.errorImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_error,
                                null
                            )
                        )
                    }

                    NO_INTERNET_CONNECTION -> {
                        binding.lytErrorScreenVideoPlayer.errorImage.setImageDrawable(
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
            binding.lytErrorScreenVideoPlayer.errorLayout.visibility = View.GONE
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
        binding.lytCustomControls.exoQuality.text = quality
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
        binding.lytCustomControls.exoSpeedText.text = speedText
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
            if (httpError is HttpDataSource.InvalidResponseCodeException) {
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
        if (isVideoPlaying) listenToRemainingTime() else job?.cancel()
        if (playbackState == Player.STATE_READY && playWhenReady) {
            binding.lytCustomControls.exoPlay.setImageResource(R.drawable.ic_media_play)
            binding.lytCustomControls.exoPause.setImageResource(R.drawable.ic_media_pause)

            playOrPausePlayer(true)

        }
        if (playbackState == Player.STATE_BUFFERING && playWhenReady) {
            binding.lytCustomControls.exoPlay.setImageResource(0)
            binding.lytCustomControls.exoPause.setImageResource(0)
            showLoading(false)
        }
        if (playbackState == Player.STATE_READY) {
            binding.exoPlayerView.videoSurfaceView?.visibility = View.VISIBLE
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
        mediaSession = MediaSession.Builder(requireContext(), player)
            .setId(TAG)
            .setCallback(MySessionCallback())
            .build()
    }

    private fun unRegisterMediaSession() {
        mediaSession.release()
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
}

class MySessionCallback : MediaSession.Callback {

}

interface VideoPlayerListener {
    fun updateWatchedValue(content: Content)
    fun playPreviousEpisode()
    fun playNextEpisode()
}