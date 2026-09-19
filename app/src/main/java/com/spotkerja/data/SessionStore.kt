package com.spotkerja.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** Penyimpanan history lokal: satu file JSON per sesi di filesDir/sessions. */
class SessionStore(context: Context) {

    private val dir = File(context.filesDir, "sessions").apply { mkdirs() }
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    suspend fun list(): List<ScanSession> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString<ScanSession>(it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.createdAtEpochMs }
            .orEmpty()
    }

    suspend fun save(session: ScanSession) = withContext(Dispatchers.IO) {
        File(dir, "${session.id}.json").writeText(json.encodeToString(ScanSession.serializer(), session))
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }
}
