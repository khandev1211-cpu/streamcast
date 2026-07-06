package com.streamcast.feature.library.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getLocalVideos(): List<MediaSource> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<MediaSource>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                videoList.add(
                    MediaSource(
                        id = id.toString(),
                        uri = contentUri,
                        type = SourceType.LOCAL,
                        displayName = name,
                        isCacheable = true
                    )
                )
            }
        }
        videoList
    }
}
