package uk.akane.accord.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import uk.akane.accord.logic.dp

class ShadowEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetView: View? = null
    private val renderNode = RenderNode("ShadowEffect")

    init {
        val blurRadius = 15.dp.px
        renderNode.setRenderEffect(
            RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.MIRROR
            )
        )
    }

    fun attach(view: View) {
        targetView = view
        updateNode()
        invalidate()
    }

    fun detach() {
        targetView = null
        invalidate()
    }

    private fun updateNode() {
        val view = targetView ?: return
        if (width == 0 || height == 0) return

        renderNode.setPosition(0, 0, width, height)
        val recordingCanvas = renderNode.beginRecording(width, height)
        
        view.draw(recordingCanvas)
        
        renderNode.endRecording()
    }

    override fun onDraw(canvas: Canvas) {
        if (targetView != null) {
            canvas.drawRenderNode(renderNode)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) updateNode()
    }
}
