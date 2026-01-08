package uk.akane.accord.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.graphics.withTranslation

class QueueBlendView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    private var targetView: View? = null

    private val renderNode = RenderNode("RenderBox")


    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRect(0, 0, view.width, view.height)
            }
        }
    }

    fun setup(target: View) {
        this.targetView = target
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        renderNode.setPosition(0, 0, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val target = targetView ?: return
        if (!isShown) return

        // Calculate offset manually by traversing up to the common parent (FullPlayer)
        // This is much faster than getLocationOnScreen()
        var offsetX = 0f
        var offsetY = 0f

        var current: android.view.ViewParent? = parent
        while (current != null && current !== target.parent) {
            if (current is View) {
                offsetX -= current.left
                offsetY -= current.top
            }
            current = current.parent
        }

        // Add target's own position in its parent
        offsetX += target.left
        offsetY += target.top

        // Record drawing commands
        val recordingCanvas = renderNode.beginRecording(width, height)
        recordingCanvas.withTranslation(offsetX, offsetY) {
            concat(target.matrix)
            target.draw(this)
        }
        renderNode.endRecording()

        canvas.drawRenderNode(renderNode)

        // Only invalidate if we are actually visible and the background is likely animating
        postInvalidateOnAnimation()
    }
}