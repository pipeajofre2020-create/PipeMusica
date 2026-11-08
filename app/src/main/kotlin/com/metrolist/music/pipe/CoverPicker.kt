/**
 * Pipe Música — personal fork of Metrolist (GPL-3.0)
 */

package com.metrolist.music.pipe

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.metrolist.music.R
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 * Lets the user pick an image from the gallery, crop it square and keep it as a cover.
 * Returns a function that starts the flow; [onCoverReady] receives the cover URI as a string.
 */
@Composable
fun rememberCoverPicker(onCoverReady: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val title = stringResource(R.string.pipe_change_cover)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cropLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val file = pendingPath?.let(::File)
            pendingPath = null
            if (result.resultCode == Activity.RESULT_OK && file != null && file.exists() && file.length() > 0) {
                onCoverReady(CoverStorage.uriFor(context, file).toString())
            }
        }

    val pickLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { source ->
            if (source == null) return@rememberLauncherForActivityResult
            val file = CoverStorage.newFile(context)
            pendingPath = file.absolutePath
            val destination = CoverStorage.uriFor(context, file)

            val options =
                UCrop.Options().apply {
                    setCompressionFormat(Bitmap.CompressFormat.JPEG)
                    setCompressionQuality(90)
                    setHideBottomControls(true)
                    setToolbarTitle(title)
                    setStatusBarLight(colors.surface.luminance() > 0.5f)
                    setToolbarColor(colors.surface.toArgb())
                    setToolbarWidgetColor(colors.inverseSurface.toArgb())
                    setRootViewBackgroundColor(colors.surface.toArgb())
                    setLogoColor(colors.surface.toArgb())
                }

            val intent =
                UCrop
                    .of(source, destination)
                    .withAspectRatio(1f, 1f)
                    .withOptions(options)
                    .getIntent(context)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(intent)
        }

    return {
        pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
