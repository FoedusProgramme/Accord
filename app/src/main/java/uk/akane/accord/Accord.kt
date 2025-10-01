package uk.akane.accord

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.webkit.MimeTypeMap
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.annotation.InternalCoilApi
import coil3.asImage
import coil3.decode.ContentMetadata
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.NullRequestDataException
import coil3.size.pxOrElse
import coil3.toCoilUri
import coil3.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import org.lsposed.hiddenapibypass.LSPass
import uk.akane.accord.logic.hasScopedStorageWithMediaTypes
import uk.akane.libphonograph.Constants
import uk.akane.libphonograph.reader.FlowReader
import uk.akane.libphonograph.utils.MiscUtils
import java.io.File
import java.io.IOException

class Accord : Application(), SingletonImageLoader.Factory {

    lateinit var reader: FlowReader
        private set

    val minSongLengthSecondsFlow = MutableStateFlow<Long>(0)
    val blackListSetFlow = MutableStateFlow<Set<String>>(setOf())
    val shouldUseEnhancedCoverReadingFlow = if (hasScopedStorageWithMediaTypes()) null else
        MutableStateFlow<Boolean?>(true)

    init {
        @SuppressLint("DefaultUncaughtExceptionDelegation")
        LSPass.setHiddenApiExemptions("")
        if (BuildConfig.DEBUG)
            System.setProperty("kotlinx.coroutines.debug", "on")
    }

    companion object {
        // not actually defined in API, but CTS tested
        // https://cs.android.com/android/platform/superproject/main/+/main:packages/providers/MediaProvider/src/com/android/providers/media/LocalUriMatcher.java;drc=ddf0d00b2b84b205a2ab3581df8184e756462e8d;l=182
        private const val MEDIA_ALBUM_ART = "albumart"
    }

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

    @OptIn(InternalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCache(null)
            .components {
                add(Fetcher.Factory { data, options, _ ->
                    if (data !is Uri) return@Factory null
                    if (data.scheme != "gramophoneSongCover") return@Factory null
                    return@Factory Fetcher {
                        val file = File(data.path!!)
                        val uri = ContentUris.appendId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(), data.authority!!.toLong()
                        ).appendPath(MEDIA_ALBUM_ART).build()
                        val bmp = if (options.size.width.pxOrElse { 0 } > 300
                            && options.size.height.pxOrElse { 0 } > 300) try {
                            ThumbnailUtils.createAudioThumbnail(file, options.size.let {
                                Size(
                                    it.width.pxOrElse { throw IllegalArgumentException("missing required size") },
                                    it.height.pxOrElse { throw IllegalArgumentException("missing required size") })
                            }, null)
                        } catch (e: IOException) {
                            if (e.message != "No embedded album art found" &&
                                e.message != "No thumbnails in Downloads directories" &&
                                e.message != "No thumbnails in top-level directories" &&
                                e.message != "No album art found")
                                throw e
                            null
                        } else null
                        if (bmp != null) {
                            ImageFetchResult(
                                bmp.asImage(), true, DataSource.DISK
                            )
                        } else {
                            if (uri == null) return@Fetcher null
                            val stream = contentResolver.openAssetFileDescriptor(uri, "r")
                            checkNotNull(stream) { "Unable to open '$uri'." }
                            SourceFetchResult(
                                source = ImageSource(
                                    source = stream.createInputStream().source().buffer(),
                                    fileSystem = options.fileSystem,
                                    metadata = ContentMetadata(uri.toCoilUri(), stream),
                                ),
                                mimeType = contentResolver.getType(uri),
                                dataSource = DataSource.DISK,
                            )
                        }
                    }
                })
                add(Fetcher.Factory { data, options, _ ->
                    if (data !is Uri) return@Factory null
                    if (data.scheme != "gramophoneAlbumCover") return@Factory null
                    return@Factory Fetcher {
                        val cover = MiscUtils.findBestCover(File(data.path!!))
                        if (cover == null) {
                            val uri =
                                ContentUris.withAppendedId(Constants.baseAlbumCoverUri, data.authority!!.toLong())
                            val contentResolver = options.context.contentResolver
                            val afd = contentResolver.openAssetFileDescriptor(uri, "r")
                            checkNotNull(afd) { "Unable to open '$uri'." }
                            return@Fetcher SourceFetchResult(
                                source = ImageSource(
                                    source = afd.createInputStream().source().buffer(),
                                    fileSystem = options.fileSystem,
                                    metadata = ContentMetadata(data, afd),
                                ),
                                mimeType = contentResolver.getType(uri),
                                dataSource = DataSource.DISK,
                            )
                        }
                        return@Fetcher SourceFetchResult(
                            ImageSource(cover.toOkioPath(), options.fileSystem, null, null, null),
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(cover.extension),
                            DataSource.DISK
                        )
                    }
                })
            }
            .run {
                if (!BuildConfig.DEBUG) this else
                    logger(object : Logger {
                        override var minLevel = Logger.Level.Verbose
                        override fun log(
                            tag: String,
                            level: Logger.Level,
                            message: String?,
                            throwable: Throwable?
                        ) {
                            if (level < minLevel) return
                            val priority = level.ordinal + 2 // obviously the best way to do it
                            if (message != null) {
                                Log.println(priority, tag, message)
                            }
                            // Let's keep the log readable and ignore normal events' stack traces.
                            if (throwable != null && throwable !is NullRequestDataException
                                && (throwable !is IOException
                                        || throwable.message != "No album art found")
                            ) {
                                Log.println(priority, tag, Log.getStackTraceString(throwable))
                            }
                        }
                    })
            }
            .build()
    }
}