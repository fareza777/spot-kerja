package com.spotkerja.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FocusLog(
    val spotLabel: String,
    val epochMs: Long,
    val minutes: Int,
)

/** Catatan menit fokus nyata per spot — satu file JSON lokal. */
class FocusStore(context: Context) {

    private val file = File(context.filesDir, "focus_logs.json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun list(): List<FocusLog> = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<List<FocusLog>>(file.readText()) }
            .getOrDefault(emptyList())
    }

    suspend fun add(log: FocusLog) = withContext(Dispatchers.IO) {
        val all = list() + log
        file.writeText(json.encodeToString(ListSerializer(FocusLog.serializer()), all))
    }

    suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }
}
