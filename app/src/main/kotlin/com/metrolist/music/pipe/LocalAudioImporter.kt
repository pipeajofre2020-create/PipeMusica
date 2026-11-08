/**
 * Pipe Música — personal fork of Metrolist (GPL-3.0)
 */

package com.metrolist.music.pipe

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.metrolist.music.R
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Imports audio files that are already on the phone into the library.
 *
 * Imported songs use their content:// URI as the song id, are marked as local and
 * downloaded, so they show up in "Descargas" and play without internet.
 */
object LocalAudioImporter {
    val AUDIO_MIME_TYPES =
        arrayOf(
            "audio/*",
        )

    fun isLocalSongId(id: String): Boolean = id.startsWith("content://") || id.startsWith("file://")

    /** @return how many files were imported successfully. */
    suspend fun import(
        context: Context,
        database: MusicDatabase,
        uris: List<Uri>,
    ): Int =
        withContext(Dispatchers.IO) {
            var imported = 0
            uris.forEach { uri ->
                runCatching {
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: SecurityException) {
                        Timber.w(e, "Could not take persistable permission for $uri")
                    }
                    val (metadata, albumName) = readMetadata(context, uri)
                    val now = LocalDateTime.now()
                    database.withTransaction {
                        insert(metadata) { song ->
                            song.copy(
                                albumName = albumName,
                                isLocal = true,
                                isDownloaded = true,
                                dateDownload = now,
                                inLibrary = now,
                            )
                        }
                    }
                }.onSuccess {
                    imported++
                }.onFailure {
                    Timber.e(it, "Failed to import $uri")
                }
            }
            imported
        }

    private fun readMetadata(
        context: Context,
        uri: Uri,
    ): Pair<MediaMetadata, String?> {
        val fileName = displayName(context, uri)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                    ?: fileName?.substringBeforeLast('.')
                    ?: context.getString(R.string.pipe_unknown_title)
            val artist =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.pipe_unknown_artist)
            val durationSeconds =
                (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000L
            val albumName =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
            val cover = retriever.embeddedPicture?.let { CoverStorage.saveBytes(context, it) }

            return MediaMetadata(
                id = uri.toString(),
                title = title,
                artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
                duration = durationSeconds.toInt(),
                thumbnailUrl = cover,
            ) to albumName
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun displayName(
        context: Context,
        uri: Uri,
    ): String? =
        runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
}
