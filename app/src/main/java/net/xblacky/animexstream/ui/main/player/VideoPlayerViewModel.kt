package net.xblacky.animexstream.ui.main.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import net.xblacky.animexstream.ui.main.player.source.EpisodeRepository
import net.xblacky.animexstream.utils.CommonViewModel
import net.xblacky.animexstream.utils.di.AppModules
import net.xblacky.animexstream.utils.di.DispatcherModule
import net.xblacky.animexstream.utils.extractors.FileUrl
import net.xblacky.animexstream.utils.extractors.VideoServer
import net.xblacky.animexstream.utils.model.Content
import net.xblacky.animexstream.utils.model.EpisodeInfo
import net.xblacky.animexstream.utils.model.M3U8FromAjaxModel
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @DispatcherModule.MainDispatcher private val dispatcher: CoroutineDispatcher,
    private val episodeRepository: EpisodeRepository,
    @AppModules.Referer private val referer: String
) : CommonViewModel() {

    //Contains Episode info and Previous/Next Episode Urls
    private var _content = MutableLiveData(Content())
    val content: LiveData<Content> = _content

    //Contains all Player Urls ex Mp4/M3U8
    private val _mediaUrls = MutableLiveData<M3U8FromAjaxModel>()
    val mediaUrls: LiveData<M3U8FromAjaxModel> = _mediaUrls

    //Used to Dynamically create referer & Ajax Base Url
    private val _cdnServer: MutableLiveData<String> = MutableLiveData(referer)
    val cdnServer: LiveData<String> = _cdnServer

    fun updateEpisodeContent(content: Content) {
        _content.value = content
    }


    private fun handleM3U8Url(data: M3U8FromAjaxModel) {
        val content = _content.value
        data.sourceMp4.forEach {
            try {
                if (!it.label.contains("Auto")) {
                    it.label = it.label.lowercase().replace(" ", "")
                }
            } catch (ignored: Exception) {
            }

        }
        data.sourceMp4.reverse()
        content?.urls = data.sourceMp4
        _content.value = content
        saveContent(content!!)
        updateLoading(false)
    }


    //// ---------------------------New Methods------------------------------------------------------

    fun fetchEpisodeData(forceRefresh: Boolean = false) {
        viewModelScope.launch(dispatcher) {
            updateLoading(true)

            try {
                content.value?.episodeUrl?.let {

                    //Request 1st to scrape Media Server Urls
                    val gogoAnimeEpisodeData =
                        episodeRepository.fetchEpisodeData(url = it, forceRefresh = forceRefresh)
                    handleEpisodeData(gogoAnimeEpisodeData)


                    //Request 2nd to VidCdn Server to decrypt AJAX URL
                    val encryptedAjaxParams =
                        episodeRepository.fetchAjaxParams(
                            url = gogoAnimeEpisodeData.vidCdnUrl,
                            forceRefresh = forceRefresh
                        )

                    //Generate Url based on Host and Params to request
                    val encryptedAjaxUrl = "${cdnServer.value}encrypt-ajax.php?$encryptedAjaxParams"
                    Timber.e(encryptedAjaxUrl)
                    val m3u8Data = episodeRepository.fetchM3U8DataFromAjax(encryptedAjaxUrl)
                    handleM3U8Url(m3u8Data!!)

                }
            } catch (exc: Exception) {
                Timber.e(exc)
                updateLoading(loading = false)
            }
        }
    }

    private fun handleEpisodeData(episodeInfo: EpisodeInfo) {
        _cdnServer.value = "https://${URI(episodeInfo.vidCdnUrl).host}/"
        Timber.e(_cdnServer.value)
        _content.value?.previousEpisodeUrl = episodeInfo.previousEpisodeUrl
        _content.value?.nextEpisodeUrl = episodeInfo.nextEpisodeUrl
        updateWatchProgress()

    }

    private fun updateWatchProgress() {
        val watchedEpisode =
            episodeRepository.getWatchDuration(_content.value?.episodeUrl.hashCode())
        _content.value?.watchedDuration = watchedEpisode?.watchedDuration ?: 0

    }

    fun saveContent(content: Content) {
        if (!content.urls.isNullOrEmpty()) {
            episodeRepository.saveWatchProgress(content)
        }
    }

}