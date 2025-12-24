package uk.akane.accord.setupwizard.components

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class LinkTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(
    context,
    attrs,
    defStyleAttr
) {
    init {
        movementMethod = LinkMovementMethod.getInstance()
    }
}