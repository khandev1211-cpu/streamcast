package com.streamcast.feature.iptv.parser

import com.streamcast.core.database.entities.Channel
import java.util.UUID

object M3UParser {

    fun parse(content: String, sourceId: String, defaultCountry: String? = null): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        
        var currentName: String? = null
        var currentLogo: String? = null
        var currentCategory: String? = null
        var currentCountry: String? = defaultCountry
        var currentEpgId: String? = null
        val currentHeaders = mutableMapOf<String, String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF")) {
                // Extract metadata from #EXTINF line
                currentName = trimmed.substringAfterLast(",").trim()
                currentLogo = extractAttribute(trimmed, "tvg-logo")
                currentCategory = extractAttribute(trimmed, "group-title")
                currentCountry = extractAttribute(trimmed, "tvg-country") ?: defaultCountry
                currentEpgId = extractAttribute(trimmed, "tvg-id")
                
                // Check for inline headers like user-agent
                extractAttribute(trimmed, "http-user-agent")?.let { currentHeaders["User-Agent"] = it }
                extractAttribute(trimmed, "http-referrer")?.let { currentHeaders["Referer"] = it }

            } else if (trimmed.startsWith("#EXTVLCOPT:")) {
                val opt = trimmed.substringAfter("#EXTVLCOPT:").trim()
                if (opt.startsWith("http-user-agent=")) {
                    currentHeaders["User-Agent"] = opt.substringAfter("http-user-agent=").trim()
                } else if (opt.startsWith("http-referrer=")) {
                    currentHeaders["Referer"] = opt.substringAfter("http-referrer=").trim()
                }
            } else if (!trimmed.startsWith("#")) {
                // This is the URL line
                if (currentName != null) {
                    channels.add(
                        Channel(
                            id = UUID.randomUUID().toString(),
                            sourceId = sourceId,
                            name = currentName,
                            logoUrl = currentLogo,
                            category = currentCategory,
                            country = currentCountry,
                            streamUrl = trimmed,
                            epgChannelId = currentEpgId,
                            headers = if (currentHeaders.isEmpty()) null else currentHeaders.toMap()
                        )
                    )
                }
                // Reset for next entry
                currentName = null
                currentLogo = null
                currentCategory = null
                currentCountry = defaultCountry
                currentEpgId = null
                currentHeaders.clear()
            }
        }
        return channels
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"(.*?)\"".toRegex()
        return pattern.find(line)?.groupValues?.get(1)
    }
}
