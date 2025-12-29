package uk.akane.accord.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.fragments.browse.AlbumsFragment
import uk.akane.accord.ui.fragments.browse.ArtistsFragment
import uk.akane.accord.ui.fragments.browse.SongFragment

class LibraryHeadAdapter(private val context: Context) : RecyclerView.Adapter<LibraryHeadAdapter.ViewHolder>() {

    private val activity: MainActivity
        get() = context as MainActivity

    enum class SectionType(val titleResId: Int, val iconResId: Int) {
        PLAYLIST(R.string.library_head_playlist, R.drawable.ic_playlist),
        ARTIST(R.string.library_head_artist, R.drawable.ic_microphone),
        ALBUM(R.string.library_head_album, R.drawable.ic_album),
        SONG(R.string.library_head_song, R.drawable.ic_music_note),
        GENRE(R.string.library_head_genre, R.drawable.ic_genre)
    }

    private val currentHeaderArrangeList = mutableListOf<SectionType>(
        SectionType.PLAYLIST,
        SectionType.ARTIST,
        SectionType.ALBUM,
        SectionType.SONG,
        SectionType.GENRE
    )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.adapter_library_section,
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.title.text = context.getString(currentHeaderArrangeList[position].titleResId)
        holder.icon.setImageResource(currentHeaderArrangeList[position].iconResId)
        holder.itemView.setOnClickListener {
            activity.fragmentSwitcherView.addFragmentToCurrentStack(
                when (currentHeaderArrangeList[holder.bindingAdapterPosition]) {
                    SectionType.SONG -> SongFragment()
                    SectionType.ALBUM -> AlbumsFragment()
                    SectionType.ARTIST -> ArtistsFragment()
                    else -> SongFragment()
                }
            )
        }
    }

    override fun getItemCount(): Int = currentHeaderArrangeList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val icon: ImageView = view.findViewById(R.id.icon)
    }
}
