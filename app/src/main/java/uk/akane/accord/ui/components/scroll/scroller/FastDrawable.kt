package uk.akane.accord.ui.components.scroll.scroller

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class FastDrawable {
    val paint = Paint()
    val bounds = RectF()
    private var cornerRadius = 0f
    private var alpha = 255
    private var colorFilter: ColorFilter? = null

    fun draw(canvas: Canvas) {
        if (cornerRadius == 0f) {
            canvas.drawRect(bounds, paint)
        } else {
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
        }
    }

    fun setAlpha(alpha: Int) {
        this.alpha = alpha
        paint.alpha = alpha
    }

    fun setColorFilter(colorFilter: ColorFilter?) {
        this.colorFilter = colorFilter
        paint.colorFilter = colorFilter
    }

    fun setBounds(left: Float, top: Float, right: Float, bottom: Float) {
        bounds.set(left, top, right, bottom)
    }

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        bounds.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    fun setBounds(bounds: RectF) {
        this.bounds.set(bounds)
    }

    fun setBounds(bounds: Rect) {
        this.bounds.set(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
    }

    fun setCornerRadius(cornerRadius: Float) {
        this.cornerRadius = cornerRadius
    }
}