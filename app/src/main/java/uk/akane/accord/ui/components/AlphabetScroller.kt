package uk.akane.accord.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import uk.akane.accord.R
import uk.akane.accord.logic.dp

class AlphabetScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = ('A'..'Z') + '#'
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = resources.getDimensionPixelSize(R.dimen.scroller_char_size).toFloat()
        color = resources.getColor(R.color.accentColor, null)
        typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
    }

    private val scrollerWidth = resources.getDimensionPixelSize(R.dimen.scroller_width)
    private val fatWidth = paint.measureText("W")

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            scrollerWidth,
            MeasureSpec.makeMeasureSpec((letters.size * (paint.textSize + SCROLLER_TEXT_PADDING.dp.px * 2)).toInt(), MeasureSpec.EXACTLY) // 文字高度 + 间距
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var y = paint.textSize + SCROLLER_TEXT_PADDING.dp.px

        letters.forEach { letter ->
            canvas.drawText(
                letter.toString(),
                width - fatWidth + (fatWidth - paint.measureText(letter.toString())) / 2,
                y,
                paint
            )
            y += paint.textSize + SCROLLER_TEXT_PADDING.dp.px * 2
        }
    }

    companion object {
        const val SCROLLER_TEXT_PADDING = 2
    }
}
