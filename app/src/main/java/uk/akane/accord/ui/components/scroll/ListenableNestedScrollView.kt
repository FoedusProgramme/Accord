package uk.akane.accord.ui.components.scroll

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

open class ListenableNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : NestedScrollView(context, attrs) {
    private val _userActionFlow = MutableStateFlow(UserAction.None)
    val userActionFlow: StateFlow<UserAction> = _userActionFlow

    private var lastTouchY = 0f

    internal fun setUserAction(userAction: UserAction) {
        _userActionFlow.tryEmit(userAction)
    }

    suspend fun collectUserAction(
        onActionStart: () -> Unit,
        onActionEnd: () -> Unit
    ) {
        var isStarted = false
        userActionFlow.collectLatest { userAction ->
            when (userAction) {
                UserAction.Drag -> {
                    if (!isStarted) {
                        onActionStart()
                        isStarted = true
                    }
                }

                UserAction.Fling -> {
                    if (!isStarted) {
                        onActionStart()
                        isStarted = true
                    }
                    var lastY = scrollY
                    var isScrollEnd = false
                    while (!isScrollEnd) {
                        delay(100)
                        val newY = scrollY
                        isScrollEnd = lastY == newY
                        lastY = newY
                    }
                    _userActionFlow.emit(UserAction.None)
                }

                UserAction.None -> {
                    onActionEnd()
                    isStarted = false
                }
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = ev.y
                _userActionFlow.tryEmit(UserAction.None)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.y - lastTouchY
                if (deltaY != 0f) {
                    _userActionFlow.tryEmit(UserAction.Drag)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                _userActionFlow.tryEmit(UserAction.None)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun fling(velocityY: Int) {
        super.fling(velocityY)
        _userActionFlow.tryEmit(UserAction.Fling)
    }

    enum class UserAction {
        None,
        Drag,
        Fling
    }
}