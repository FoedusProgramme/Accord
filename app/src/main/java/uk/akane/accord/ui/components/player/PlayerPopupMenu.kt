package uk.akane.accord.ui.components.player

import android.content.res.Resources
import uk.akane.accord.R
import uk.akane.accord.ui.components.PopupHelper

object PlayerPopupMenu {
    fun build(resources: Resources): PopupHelper.PopupEntries {
        return PopupHelper.PopupMenuBuilder()
            .addMenuEntry(resources, R.drawable.ic_info, R.string.popup_view_credits)
            .addSpacer()
            .addDestructiveMenuEntry(
                resources,
                R.drawable.ic_trash,
                R.string.popup_delete_from_library
            )
            .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_add_to_a_playlist)
            .addSpacer()
            .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_share_song)
            .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_share_lyrics)
            .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_go_to_album)
            .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_create_station)
            .addSpacer()
            .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_undo_favorite)
            .build()
    }
}
