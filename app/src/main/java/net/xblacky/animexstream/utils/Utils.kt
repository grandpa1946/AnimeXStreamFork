package net.xblacky.animexstream.utils

import android.content.Context
import android.util.DisplayMetrics
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.xblacky.animexstream.utils.constants.C
import net.xblacky.animexstream.utils.extractors.FileUrl
import net.xblacky.animexstream.utils.preference.PreferenceHelper
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException


class Utils {
    companion object {

        fun getRemainingTime(watchedDuration: Long, totalDuration: Long): String {
            return if (totalDuration > watchedDuration) {
                val remainingDuration = totalDuration - watchedDuration
                val s: Long = remainingDuration / 1000 % 60
                val m: Long = remainingDuration / 1000 / 60
                String.format("%02d:%02d", m, s)
            } else {
                "00:00"
            }


        }

        fun getWatchedDuration(watchedDuration: Long): String {
            val s: Long = watchedDuration / 1000 % 60
            val m: Long = watchedDuration / 1000 / 60
            return String.format("%02d:%02d", m, s)
        }

        fun getTypeName(typeValue: Int): String {

            return when (typeValue) {

                C.TYPE_RECENT_DUB -> "Recent Dub"
                C.TYPE_RECENT_SUB -> "Recent Sub"
                C.TYPE_MOVIE -> "Movies"
                C.TYPE_POPULAR_ANIME -> "Popular Anime"
                C.TYPE_GENRE -> "Categories"
                C.TYPE_NEW_SEASON -> "New Season"
                else -> "Default"
            }
        }

        fun getHeader(): Map<String, String> {
            val pref = PreferenceHelper.sharedPreference
            return mapOf(
                "referer" to pref.getReferrer(),
                "origin" to pref.getOrigin(),
                "user-agent" to C.USER_AGENT
            )
        }

        fun calculateNoOfColumns(
            context: Context,
            columnWidthDp: Float
        ): Int {
            val displayMetrics: DisplayMetrics = context.resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return (screenWidthDp / columnWidthDp + 0.5).toInt()
        }

        fun getPositionByType(typeValue: Int): Int {
            return when (typeValue) {
                C.TYPE_RECENT_SUB -> C.RECENT_SUB_POSITION
                C.TYPE_NEW_SEASON -> C.NEWEST_SEASON_POSITION
                C.TYPE_RECENT_DUB -> C.RECENT_SUB_POSITION
                C.TYPE_MOVIE -> C.MOVIE_POSITION
                C.TYPE_POPULAR_ANIME -> C.POPULAR_POSITION
                else -> 0
            }
        }

    }
}

fun <A, B> Collection<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

suspend fun <T> tryWithSuspend(call: suspend () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Throwable) {
        Timber.e(e)
        null
    } catch (e: CancellationException) {
        null
    }
}

fun String.findBetween(a: String, b: String): String? {
    val start = this.indexOf(a)
    val end = if (start != -1) this.indexOf(b, start) else return null
    return if (end != -1) this.subSequence(start, end).removePrefix(a).removeSuffix(b).toString() else null
}

suspend fun getSize(file: FileUrl): Double? {
    return tryWithSuspend {
        client.head(file.url, file.headers, timeout = 1000).size?.toDouble()?.div(1024 * 1024)
    }
}