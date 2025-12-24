package uk.akane.accord.logic.utils

import android.content.Context
import android.util.TypedValue

object UiUtils {

    var density: Float = 3f
        private set
    var scaledDensity: Float = 3f
        private set

    data class ScreenCorners(
        val topLeft: Float,
        val topRight: Float,
        val bottomLeft: Float,
        val bottomRight: Float
    ) {
        fun getAvgRadius() =
            (topLeft + topRight + bottomLeft + bottomRight) / 4f
    }

    fun init(context: Context) {
        val resources = context.resources
        density = resources.displayMetrics.density
        scaledDensity = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            1f,
            resources.displayMetrics
        )
    }

}