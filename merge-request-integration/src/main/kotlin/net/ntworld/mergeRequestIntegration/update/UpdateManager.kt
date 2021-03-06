package net.ntworld.mergeRequestIntegration.update

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import java.util.*

object UpdateManager {
    private const val CURRENT_VERSION = "2019.3.1"
    private const val METADATA_URL = "https://nhat-phan.github.io/updates/merge-request-integration/metadata.json"
    private const val CHECK_INTERVAL = 3600000 // Every 1 hour

    private val myJson: Json = Json(JsonConfiguration.Stable.copy(strictMode = false))
    private var myLastCheckDate : Date? = null

    fun shouldGetAvailableUpdates(): Boolean {
        val lastCheck = myLastCheckDate
        if (null === lastCheck) {
            return true
        }
        val difference = Date().time - lastCheck.time
        return difference > CHECK_INTERVAL
    }

    fun getAvailableUpdates(): List<String> {
        try {
            val (_, _, result) = Fuel.get(METADATA_URL).responseString()
            return when (result) {
                is Result.Success -> {
                    myLastCheckDate = Date()
                    buildAvailableUpdates(result.value)
                }
                is Result.Failure -> {
                    myLastCheckDate = Date()
                    listOf()
                }
            }
        } catch (exception: Exception) {
            myLastCheckDate = Date()
            return listOf()
        }
    }

    private fun buildAvailableUpdates(input: String): List<String> {
        val metadata = myJson.parse(UpdateMetadata.serializer().list, input).sortedBy { it.id }
        val currentVersion = metadata.firstOrNull { it.version == CURRENT_VERSION }
        if (null === currentVersion) {
            return listOf()
        }
        val updates = metadata.filter { it.id > currentVersion.id && currentVersion.active }
        return updates.map {
            try {
                val (_, _, result) = Fuel.get(it.changesUrl).responseString()
                when (result) {
                    is Result.Success -> {
                        result.value
                            .replace("<html lang=\"en\">", "<h2>${it.version}</h2>")
                            .replace("</html>", "")
                    }
                    is Result.Failure -> {
                        ""
                    }
                }
            } catch (exception: Exception) {
                ""
            }
        }
    }
}