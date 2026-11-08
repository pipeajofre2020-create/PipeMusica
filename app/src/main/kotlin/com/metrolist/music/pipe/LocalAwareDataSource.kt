/**
 * Pipe Música — personal fork of Metrolist (GPL-3.0)
 */

package com.metrolist.music.pipe

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener

/**
 * Sends content:// and file:// URIs (songs imported from the phone) to a local data source,
 * and everything else (YouTube ids) to the normal streaming/caching pipeline.
 */
@OptIn(UnstableApi::class)
class LocalAwareDataSourceFactory(
    context: Context,
    private val remoteFactory: DataSource.Factory,
) : DataSource.Factory {
    private val localFactory = DefaultDataSource.Factory(context.applicationContext)

    override fun createDataSource(): DataSource =
        LocalAwareDataSource(
            local = localFactory.createDataSource(),
            remote = remoteFactory.createDataSource(),
        )
}

@OptIn(UnstableApi::class)
private class LocalAwareDataSource(
    private val local: DataSource,
    private val remote: DataSource,
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        local.addTransferListener(transferListener)
        remote.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val scheme = dataSpec.uri.scheme
        val source = if (scheme == "content" || scheme == "file") local else remote
        active = source
        return source.open(dataSpec)
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int = active?.read(buffer, offset, length) ?: throw IllegalStateException("DataSource is not open")

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders ?: emptyMap()

    override fun close() {
        try {
            active?.close()
        } finally {
            active = null
        }
    }
}
