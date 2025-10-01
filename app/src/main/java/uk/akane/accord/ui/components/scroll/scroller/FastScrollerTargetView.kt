package uk.akane.accord.ui.components.scroll.scroller

interface FastScrollerTargetView {
    val viewScrollState: ScrollState

    fun setScrollState(state: ScrollState)

    fun nestedScrollBy(x: Int, y: Int)

    fun computeVerticalScrollRange(): Int
    fun computeVerticalScrollOffset(): Int
    fun computeVerticalScrollExtent(): Int
}
