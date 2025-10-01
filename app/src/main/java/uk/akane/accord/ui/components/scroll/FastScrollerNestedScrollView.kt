package uk.akane.accord.ui.components.scroll

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import uk.akane.accord.ui.components.scroll.scroller.FastScroller
import uk.akane.accord.ui.components.scroll.scroller.FastScrollerTargetView
import uk.akane.accord.ui.components.scroll.scroller.ScrollState

class FastScrollerNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ListenableNestedScrollView(context, attrs), FastScrollerTargetView {
    private val fastScroller = FastScroller(this)

    override val viewScrollState: ScrollState
        get() = when (userActionFlow.value) {
            UserAction.Drag -> ScrollState.Dragging
            UserAction.Fling -> ScrollState.Settling
            UserAction.None -> ScrollState.Idle
        }

    init {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        lifecycle?.coroutineScope?.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                collectUserAction(
                    onActionStart = {
                        if (canScrollVertically(1)) {
                            fastScroller.show()
                        }
                    },
                    onActionEnd = {
                        fastScroller.hide()
                    }
                )
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        fastScroller.updateBounds()
        if (!canScrollVertically(1)) {
            fastScroller.hideScrollerImmediately()
        } else {
            fastScroller.hide()
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.translate(0f, scrollY.toFloat())
        fastScroller.draw(canvas)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        return if (fastScroller.onInterceptTouchEvent(e)) {
            true
        } else {
            super.onInterceptTouchEvent(e)
        }
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        return if (fastScroller.onTouchEvent(e)) {
            true
        } else {
            super.onTouchEvent(e)
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        fastScroller.updateBounds()
    }

    override fun nestedScrollBy(x: Int, y: Int) {
        scrollBy(x, y)
    }

    override fun setScrollState(state: ScrollState) {
        val userAction = when (state) {
            ScrollState.Idle -> UserAction.None
            ScrollState.Dragging -> UserAction.Drag
            ScrollState.Settling -> UserAction.Fling
        }
        setUserAction(userAction)
    }
}
