package uk.akane.accord.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.graphics.withTranslation
import androidx.core.view.isVisible

class QueueBlendView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    private var targetView: View? = null
    private val locationBuffer = IntArray(2)
    private val targetLocationBuffer = IntArray(2)

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
        val parent = target.parent as? View ?: return

        // Calculate relative position of the target's parent (FullPlayer) to this view
        getLocationOnScreen(locationBuffer)
        parent.getLocationOnScreen(targetLocationBuffer)

        val dx = (targetLocationBuffer[0] - locationBuffer[0]).toFloat()
        val dy = (targetLocationBuffer[1] - locationBuffer[1]).toFloat()

        // Record drawing commands from target view
        val recordingCanvas = renderNode.beginRecording(width, height)
        recordingCanvas.withTranslation(dx, dy) {
            // 1. Move to Parent's (0,0)
            // 2. Move to Target's (left, top)
            translate(target.left.toFloat(), target.top.toFloat())
            // 3. Apply Target's transform (scale, etc.)
            concat(target.matrix)

            target.draw(this)

        }
        renderNode.endRecording()

        // Draw the blurred render node
        canvas.drawRenderNode(renderNode)

        // Keep animating if target is animating (simple approach: invalidate every frame if visible)
        if (isVisible) {
            postInvalidateOnAnimation()
        }
    }
}