package uk.akane.accord.ui.adapters

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.libphonograph.items.Genre

class SearchAdapter(
    context: Context,
    fragment: Fragment,
    callback: (() -> Unit)
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private var list: List<Genre> = emptyList()

    private val colorPalette = listOf(
        context.getColor(R.color.systemRed),
        context.getColor(R.color.systemOrange),
        context.getColor(R.color.systemYellow),
        context.getColor(R.color.systemGreen),
        context.getColor(R.color.systemMint),
        context.getColor(R.color.systemTeal),
        context.getColor(R.color.systemCyan),
        context.getColor(R.color.systemBlue),
        context.getColor(R.color.systemIndigo),
        context.getColor(R.color.systemPurple),
        context.getColor(R.color.systemPink),
        context.getColor(R.color.systemBrown),
    )

    private val colorFilters: List<ColorMatrixColorFilter> =
        colorPalette.shuffled().map { createMonochromeFilter(it) }

    init {
        fragment.lifecycleScope.launch {
            (fragment.activity as MainActivity).reader?.genreListFlow?.collectLatest { newList ->
                if (newList.isNotEmpty()) {
                    callback.invoke()
                }
                submitList(newList)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_search_genre, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val actualPosition = position % colorFilters.size
        holder.imageView.colorFilter = colorFilters[actualPosition]

        val item = list[position]
        holder.imageView.load(item.songList.first().mediaMetadata.artworkUri) {
            crossfade(true)
        }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    private fun submitList(newList: List<Genre>) {
        val diffResult = DiffUtil.calculateDiff(GenreDiffCallback(list, newList))
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    class GenreDiffCallback(
        private val oldList: List<Genre>,
        private val newList: List<Genre>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    private fun createMonochromeFilter(color: Int): ColorMatrixColorFilter {
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val matrix = ColorMatrix(
            floatArrayOf(
                0.299f * r, 0.587f * r, 0.114f * r, 0f, 0f, // R'
                0.299f * g, 0.587f * g, 0.114f * g, 0f, 0f, // G'
                0.299f * b, 0.587f * b, 0.114f * b, 0f, 0f, // B'
                0f,        0f,        0f,        1f, 0f    // A
            )
        )
        return ColorMatrixColorFilter(matrix)
    }
}
