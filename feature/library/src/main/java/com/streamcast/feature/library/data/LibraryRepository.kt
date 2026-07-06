package com.streamcast.feature.library.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import com.streamcast.feature.library.domain.model.MediaFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getMediaFolders(): List<MediaFolder> = withContext(Dispatchers.IO) {
        val foldersMap = mutableMapOf<String, MutableList<MediaSource>>()
        
        // Scan Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val data = cursor.getString(dataColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                
                val folderPath = File(data).parent ?: "Internal Storage"
                val folderName = File(folderPath).name
                
                val mediaSource = MediaSource(id.toString(), contentUri, SourceType.LOCAL, name, true)
                foldersMap.getOrPut(folderPath) { mutableListOf() }.add(mediaSource)
            }
        }

        // Scan Audio
        val audioProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioProjection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val data = cursor.getString(dataColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                
                val folderPath = File(data).parent ?: "Internal Storage"
                
                val mediaSource = MediaSource(id.toString(), contentUri, SourceType.LOCAL, name, true)
                foldersMap.getOrPut(folderPath) { mutableListOf() }.add(mediaSource)
            }
        }
        
        foldersMap.map { (path, items) ->
            MediaFolder(
                name = File(path).name,
                path = path,
                mediaCount = items.size,
                items = items
            )
        }.sortedBy { it.name }
    }
}
