package uk.akane.accord.logic.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.FileNotFoundException
import java.io.InputStream

object ImageUtils {
    fun ImageView.load(uri: Uri) {
        val bitmap = getBitmapFromUri(contentResolver = this.context.contentResolver, uri, this.height)
        bitmap?.let {
            this.setImageBitmap(bitmap)
        }
    }
    fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri, desiredSize: Int): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, desiredSize)
            options.inJustDecodeBounds = false
            inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
        }
    }
    private fun calculateInSampleSize(options: BitmapFactory.Options, desiredSize: Int): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > desiredSize || width > desiredSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= desiredSize && (halfWidth / inSampleSize) >= desiredSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}