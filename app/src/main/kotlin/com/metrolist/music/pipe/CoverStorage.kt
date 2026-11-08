/**
 * Pipe Música — personal fork of Metrolist (GPL-3.0)
 */

package com.metrolist.music.pipe

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Stores cover images chosen by the user (or embedded in local audio files)
 * inside the app's private files, so they survive cache cleaning.
 */
object CoverStorage {
    private const val DIR = "pipe_covers"
    private const val MARKER = ".FileProvider/$DIR/"

    /** True when the thumbnail was set by the user and must not be replaced by YouTube data. */
    fun isCustomCover(url: String?): Boolean = url != null && url.contains(MARKER)

    fun newFile(context: Context): File {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun uriFor(
        context: Context,
        file: File,
    ): Uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)

    fun saveBytes(
        context: Context,
        bytes: ByteArray,
    ): String {
        val file = newFile(context)
        file.writeBytes(bytes)
        return uriFor(context, file).toString()
    }
}
