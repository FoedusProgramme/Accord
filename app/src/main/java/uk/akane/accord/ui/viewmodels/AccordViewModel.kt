package uk.akane.accord.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import uk.akane.libphonograph.items.Album
import uk.akane.libphonograph.items.Artist
import uk.akane.libphonograph.items.Date
import uk.akane.libphonograph.items.FileNode
import uk.akane.libphonograph.items.Genre

class AccordViewModel() : ViewModel() {
    // MediaStore
    val mediaItemList: MutableLiveData<List<MediaItem>> = MutableLiveData()
    val albumItemList: MutableLiveData<List<Album>> = MutableLiveData()
    val albumArtistItemList: MutableLiveData<List<Artist>> = MutableLiveData()
    val artistItemList: MutableLiveData<List<Artist>> = MutableLiveData()
    val genreItemList: MutableLiveData<List<Genre>> = MutableLiveData()
    val dateItemList: MutableLiveData<List<Date>> = MutableLiveData()
    val folderStructure: MutableLiveData<FileNode> = MutableLiveData()
    val shallowFolderStructure: MutableLiveData<FileNode> = MutableLiveData()
    val allFolderSet: MutableLiveData<Set<String>> = MutableLiveData()
}