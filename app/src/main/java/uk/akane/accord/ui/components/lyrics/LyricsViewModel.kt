package uk.akane.accord.ui.components.lyrics

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.doOnLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.akane.accord.logic.forEachChild
import uk.akane.accord.ui.components.FadingVerticalEdgeLayout
import kotlin.math.roundToInt

class LyricsViewModel(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val lyrics = MutableStateFlow(Lyrics.Empty)

    private val sampleLyrics = Lyrics(
        listOf(
            LyricsLine(0, null, "Standing for so long time", null),
            LyricsLine(5_000, null, "With the wind from the mother earth", null),
            LyricsLine(10_000, null, "Spring, summer, fall and winter", null),
            LyricsLine(15_000, null, "Take me in the smell of the four season", null),
            LyricsLine(20_000, null, "Listening to the sounds of lives", null),
            LyricsLine(25_000, null, "With the rains from the father sky", null),
            LyricsLine(30_000, null, "Green, yellow, red, and brown", null),
            LyricsLine(35_000, null, "Paints me colors of the four season", null),
            LyricsLine(40_000, null, "Run and through this forest", null),
            LyricsLine(45_000, null, "With an arrow from the brother sun", null),
            LyricsLine(50_000, null, "Trees, weeds, leaves, and flowers", null),
            LyricsLine(55_000, null, "Feel we the breathe of the four season", null),
        )
    )

    fun onViewCreated(view: View, savedInstanceState: Bundle? = null) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle

        val fadingEdgeLayout = view as FadingVerticalEdgeLayout
        val scrollView = fadingEdgeLayout.getChildAt(0) as NestedScrollView
        val lyricsView = scrollView.getChildAt(0) as LyricsView

        var isUserScrolling = false

        /*
        scope.launch {
            lifecycle?.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                scrollView.collectUserAction(
                    onActionStart = {
                        scrollView.isVerticalScrollBarEnabled = true
                        lyricsView.forEachChild { child ->
                            child as LyricsLineView
                            child.animations.cancelBlur()
                            child.visibility = View.VISIBLE
                        }
                        isUserScrolling = true
                    },
                    onActionEnd = {
                        isUserScrolling = false
                    }
                )
            }
        }

         */

        fun updateCurrentIndex(index: Int) {
            Log.d("TAG", "ci: $index")
            if (!isUserScrolling) {
                val currentLineChild = lyricsView.getChildAt(index) as LyricsLineView? ?: return
                val currentOffset = scrollView.scrollY.toFloat()
                val maxScrollOffset =
                    (lyricsView.measuredHeight + lyricsView.contentPaddingTop - scrollView.measuredHeight).toFloat()
                val targetOffset =
                    currentLineChild.animations.getGlobalOffset().coerceAtMost(maxScrollOffset) -
                            lyricsView.contentPaddingTop
                Log.d("TAG", "co: $currentOffset, mso: $maxScrollOffset, to: $targetOffset")
                val deltaOffset = targetOffset - currentOffset

                scrollView.isVerticalScrollBarEnabled = false
                scrollView.scrollTo(0, targetOffset.roundToInt())

                val scrollOffset = scrollView.scrollY.toFloat()
                lyricsView.forEachChild { child ->
                    child as LyricsLineView
                    val targetTransitionY = child.textOffset + deltaOffset
                    if (child.animations.checkIsInScreen(scrollOffset, targetTransitionY)) {
                        child.textOffset = targetTransitionY
                        child.animations.update(index)
                        if (child.visibility != View.VISIBLE) {
                            child.visibility = View.VISIBLE
                        }
                    } else {
                        if (child.visibility != View.GONE) {
                            child.visibility = View.GONE
                        }
                        child.animations.updateImmediately(index)
                    }
                }
            } else {
                lyricsView.forEachChild { child ->
                    child as LyricsLineView
                    child.textOffset = 0f
                    child.animations.update(index, true)
                }
            }
        }

        var isLayoutFinished = false
        fun updateOnLayout() {
            lyricsView.doOnLayout {
                isLayoutFinished = false

                val index = 0

                val currentLineChild = lyricsView.getChildAt(index) as? LyricsLineView ?: return@doOnLayout
                val targetOffset = currentLineChild.animations.getGlobalOffset() - lyricsView.contentPaddingTop

                scrollView.isVerticalScrollBarEnabled = false
                scrollView.scrollTo(0, targetOffset.roundToInt())

                lyricsView.forEachChild { child ->
                    child as LyricsLineView
                    child.animations.updateImmediately(index)
                    child.visibility = View.VISIBLE
                }

                isLayoutFinished = true
            }
        }

        lyrics.value = sampleLyrics
        lyricsView.update(sampleLyrics)
        updateOnLayout()

        val timeOffset = 200L
        scope.launch {
            while (isActive) {
                val currentLyrics = lyrics.value
                if (currentLyrics != Lyrics.Empty) {
                    val totalLines = currentLyrics.lyrics.size
                    for (index in 0 until totalLines) {
                        if (!isLayoutFinished) break
                        updateCurrentIndex(index)
                        delay(5000L)
                    }
                }
                delay(1000L)
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun getCurrentLyricsLineIndex(position: Long): Int {
        val currentLyrics = lyrics.value
        val line = currentLyrics.lyrics.indexOfLast { it.timestamp <= position }
        return if (line != -1) line else 0
    }
}