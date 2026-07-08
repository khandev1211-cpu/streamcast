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
    /**
     * Scans and returns media folders.
     * @param rootPath if null, returns top-level folders that contain media.
     * @param hierarchical if true, respects the file system hierarchy.
     */
    suspend fun getMediaFolders(rootPath: String? = null, hierarchical: Boolean = true): List<MediaFolder> = withContext(Dispatchers.IO) {
        val allMedia = mutableListOf<Pair<String, MediaSource>>()
        
        // Scan Videos
        val videoProjection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATA)
        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoProjection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                allMedia.add(path to MediaSource(cursor.getLong(idCol).toString(), uri, SourceType.LOCAL, cursor.getString(nameCol), true))
            }
        }

        // Scan Audio
        val audioProjection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA)
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioProjection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                allMedia.add(path to MediaSource(cursor.getLong(idCol).toString(), uri, SourceType.LOCAL, cursor.getString(nameCol), true))
            }
        }

        if (!hierarchical) {
            // Flat view: if rootPath is null, show all folders. If not null, show nothing (items are in the folder already)
            if (rootPath != null) return@withContext emptyList()

            return@withContext allMedia.groupBy { File(it.first).parent ?: "Internal Storage" }
                .map { (path, items) ->
                    MediaFolder(name = File(path).name, path = path, mediaCount = items.size, items = items.map { it.second })
                }.sortedBy { it.name }
        } else {
            // Hierarchical view logic
            val filteredMedia = if (rootPath == null) allMedia else allMedia.filter { it.first.startsWith(rootPath) }
            
            val folders = mutableMapOf<String, MutableList<MediaSource>>()
            val subFoldersPaths = mutableSetOf<String>()

            filteredMedia.forEach { (fullPath, media) ->
                val file = File(fullPath)
                val parentPath = file.parent ?: ""
                
                if (rootPath == null) {
                    // In hierarchical mode at root, we show top-level directories that contain media
                    // Simplified: just show all folders like flat mode for now
                    folders.getOrPut(parentPath) { mutableListOf() }.add(media)
                } else {
                    if (parentPath == rootPath) {
                        folders.getOrPut(parentPath) { mutableListOf() }.add(media)
                    } else {
                        val relative = fullPath.substringAfter(rootPath).trimStart(File.separatorChar)
                        val directSub = relative.substringBefore(File.separatorChar)
                        if (directSub.isNotEmpty() && directSub != file.name) {
                            subFoldersPaths.add(File(rootPath, directSub).absolutePath)
                        }
                    }
                }
            }

            if (rootPath == null) {
                return@withContext allMedia.groupBy { File(it.first).parent ?: "Internal Storage" }
                    .map { (path, items) ->
                        MediaFolder(name = File(path).name, path = path, mediaCount = items.size, items = items.map { it.second })
                    }.sortedBy { it.name }
            }

            val result = mutableListOf<MediaFolder>()
            subFoldersPaths.forEach { path ->
                val count = allMedia.count { it.first.startsWith(path) }
                if (count > 0) {
                    result.add(MediaFolder(name = File(path).name, path = path, mediaCount = count))
                }
            }

            return@withContext (result + folders.map { (path, items) -> 
                MediaFolder(name = File(path).name, path = path, mediaCount = items.size, items = items)
            }).distinctBy { it.path }.sortedBy { it.name }
        }
    }
}
