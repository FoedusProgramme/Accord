package uk.akane.accord.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.fragments.browse.SongFragment

class BrowseAdapter(
    private val context: Context
) : RecyclerView.Adapter<BrowseAdapter.ViewHolder>() {

    private val activity: MainActivity
        get() = context as MainActivity

    private val browseList: List<BrowseListItem> = listOf(
        BrowseListItem.Header(context.getString(R.string.explore_more)),
        BrowseListItem.SideAction(context.getString(R.string.browse_by_name), FragmentType.Song)
    )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(
            when (viewType) {
                VIEW_TYPE_HEADER -> R.layout.adapter_browse_header
                VIEW_TYPE_SIDE_ACTION -> R.layout.adapter_browse_side_action
                else -> throw IllegalArgumentException("viewType not found: $viewType")
            },
            parent,
            false
        ))

    override fun getItemViewType(position: Int): Int =
        when (browseList[position]) {
            is BrowseListItem.Header -> VIEW_TYPE_HEADER
            is BrowseListItem.SideAction -> VIEW_TYPE_SIDE_ACTION
        }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.title?.text = browseList[position].title
        if (browseList[position] is BrowseListItem.SideAction) {
            holder.itemView.setOnClickListener {
                activity.fragmentSwitcherView.addFragmentToCurrentStack(
                    when((browseList[position] as BrowseListItem.SideAction).fragment) {
                        FragmentType.Song -> SongFragment()
                        else -> throw IllegalArgumentException("Unknown fragment type!")
                    }
                )
            }
        }
    }

    override fun getItemCount(): Int =
        browseList.size

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val title: TextView? = view.findViewById(R.id.title)
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_SIDE_ACTION = 1
    }

    sealed class BrowseListItem {
        abstract val title: String

        data class Header(
            override val title: String
        ) : BrowseListItem()

        data class SideAction(
            override val title: String,
            val fragment: FragmentType
        ) : BrowseListItem()
    }

    enum class FragmentType {
        Song, Album, Artist, Genre, ReleaseDate, Folder, Filesystem, Playlist
    }

}