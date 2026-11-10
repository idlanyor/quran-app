package com.irengcloud.quran.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun alafasyVerseUrl(surah: Int, verse: Int): String {
    val chapter = surah.toString().padStart(3, '0')
    val ayat = verse.toString().padStart(3, '0')
    return "https://everyayah.com/data/Alafasy_128kbps/$chapter$ayat.mp3"
}

data class SavedMurottal(val verseCount: Int, val bytes: Long)

class MurottalStorage(
    private val root: File,
    private val client: OkHttpClient = OkHttpClient()
) {
    fun localAudioPath(surah: Int, verse: Int): String? {
        if (surah !in 1..114 || verse < 1) return null
        return verseFile(surah, verse).takeIf { it.isFile && it.length() > 0L }?.absolutePath
    }

    suspend fun savedSurahs(): Map<Int, SavedMurottal> = withContext(Dispatchers.IO) {
        root.listFiles().orEmpty().mapNotNull { folder ->
            val surah = folder.name.toIntOrNull()?.takeIf { it in 1..114 } ?: return@mapNotNull null
            val files = folder.listFiles().orEmpty().filter {
                it.isFile && it.extension == "mp3" && it.length() > 0L
            }
            if (files.isEmpty()) null else surah to SavedMurottal(files.size, files.sumOf(File::length))
        }.toMap()
    }

    suspend fun saveSurah(surah: Int, verseCount: Int, onProgress: (Int) -> Unit) =
        withContext(Dispatchers.IO) {
            val folder = surahDirectory(surah)
            if (!folder.mkdirs() && !folder.isDirectory) throw IOException("Penyimpanan tidak tersedia")
            var saved = (1..verseCount).count { localAudioPath(surah, it) != null }
            onProgress(saved)
            for (verse in 1..verseCount) {
                if (localAudioPath(surah, verse) != null) continue
                val target = verseFile(surah, verse)
                download(alafasyVerseUrl(surah, verse), target)
                onProgress(++saved)
            }
        }

    suspend fun removeSurah(surah: Int): Boolean = withContext(Dispatchers.IO) {
        surahDirectory(surah).deleteRecursively()
    }

    private fun surahDirectory(surah: Int): File {
        require(surah in 1..114)
        return File(root, surah.toString())
    }

    private fun verseFile(surah: Int, verse: Int): File = File(surahDirectory(surah), "$verse.mp3")

    private suspend fun download(url: String, target: File) = suspendCancellableCoroutine<Unit> { continuation ->
        val call = client.newCall(Request.Builder().url(url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        val body = response.body
                        if (!response.isSuccessful || body == null) throw IOException("Audio tidak tersedia")
                        val contentType = body.contentType()
                        if (contentType != null && contentType.type != "audio" &&
                            contentType.toString() != "application/octet-stream"
                        ) {
                            throw IOException("Format audio tidak valid")
                        }
                        val temp = File.createTempFile("ayat-", ".part", target.parentFile)
                        try {
                            val bytes = temp.outputStream().buffered().use { output -> body.byteStream().copyTo(output) }
                            val expected = body.contentLength()
                            if (bytes == 0L || (expected >= 0 && bytes != expected) || call.isCanceled()) {
                                throw IOException("Audio belum selesai diunduh")
                            }
                            if (!temp.renameTo(target)) throw IOException("Gagal menyimpan audio")
                        } finally {
                            temp.delete()
                        }
                    }
                    continuation.resume(Unit)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}
