package com.streamcast.feature.library.data

import android.content.Context
import android.os.Environment
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject

@OptIn(UnstableApi::class)
class MediaConverter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun convertVideoToAudio(inputUri: android.net.Uri, outputName: String): Flow<ConversionState> = callbackFlow {
        val outputFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "$outputName.mp3"
        )
        
        val transformer = Transformer.Builder(context)
            .build()

        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                trySend(ConversionState.Success(outputFile.absolutePath))
                close()
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                trySend(ConversionState.Error(exportException.message ?: "Unknown error"))
                close()
            }
        }

        transformer.addListener(listener)
        transformer.start(editedMediaItem, outputFile.absolutePath)
        
        trySend(ConversionState.Loading(0f))

        awaitClose {
            transformer.cancel()
        }
    }
}

sealed class ConversionState {
    data class Loading(val progress: Float) : ConversionState()
    data class Success(val path: String) : ConversionState()
    data class Error(val message: String) : ConversionState()
}
