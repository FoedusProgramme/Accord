package uk.akane.accord

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import uk.akane.accord.logic.hasScopedStorageWithMediaTypes
import uk.akane.libphonograph.reader.FlowReader

class Accord : Application() {

    lateinit var reader: FlowReader
        private set

    val minSongLengthSecondsFlow = MutableStateFlow<Long>(0)
    val blackListSetFlow = MutableStateFlow<Set<String>>(setOf())
    val shouldUseEnhancedCoverReadingFlow = if (hasScopedStorageWithMediaTypes()) null else
        MutableStateFlow<Boolean?>(true)

    override fun onCreate() {
        super.onCreate()
        reader = FlowReader(
            this,
            MutableStateFlow(0),
            blackListSetFlow,
            if (hasScopedStorageWithMediaTypes()) MutableStateFlow(null) else
                shouldUseEnhancedCoverReadingFlow!!,
            // TODO: Change this into a setting later
            minSongLengthSecondsFlow,
            MutableStateFlow(true),
            "gramophoneAlbumCover"
        )
    }
}