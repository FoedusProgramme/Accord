package uk.akane.accord.logic.utils

object SortedUtils {
    data class Item<T> (
        val title: String?,
        val content: T?
    )
}