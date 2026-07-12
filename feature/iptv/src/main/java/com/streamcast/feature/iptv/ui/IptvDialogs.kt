package com.streamcast.feature.iptv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player

@Composable
fun TrackSelectionDialog(
    player: Player?,
    onDismiss: () -> Unit
) {
    if (player == null) return
    
    val tracks = player.currentTracks
    val audioTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val textTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

    var selectedTabIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tracks") },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Audio") })
                    Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Subtitles") })
                }
                
                Spacer(Modifier.height(8.dp))
                
                val currentGroups = if (selectedTabIndex == 0) audioTracks else textTracks
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (currentGroups.isEmpty()) {
                        item { Text("No tracks available", modifier = Modifier.padding(16.dp)) }
                    }
                    
                    items(currentGroups) { group ->
                        for (i in 0 until group.length) {
                            val track = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            
                            ListItem(
                                headlineContent = { 
                                    Text(track.label ?: track.language ?: "Track ${i + 1}") 
                                },
                                supportingContent = {
                                    Text("${track.sampleMimeType}")
                                },
                                trailingContent = {
                                    RadioButton(selected = isSelected, onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(
                                                androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)
                                            )
                                            .build()
                                    })
                                },
                                modifier = Modifier.clickable {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(
                                            androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)
                                        )
                                        .build()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(0, 15, 30, 60, 90).forEach { mins ->
                    val label = if (mins == 0) "Off" else "$mins minutes"
                    ListItem(
                        headlineContent = { Text(label) },
                        modifier = Modifier.clickable { onSelect(mins) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
